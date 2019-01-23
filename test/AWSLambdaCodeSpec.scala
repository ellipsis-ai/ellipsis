import java.io.{ByteArrayInputStream, File}
import java.time.OffsetDateTime

import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.ellipsisobject._
import models.team.Team
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import services.{AWSLambdaInvocationJsonBuilder, AWSLambdaZipBuilder, ApiConfigInfo}
import support.TestContext

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.sys.process._

class AWSLambdaCodeSpec extends PlaySpec with MockitoSugar {

  val actionResult: String = "foo"

  def behaviorVersionFor(team: Team): BehaviorVersion = BehaviorVersion(
    IDs.next,
    mock[Behavior],
    groupVersionFor(team),
    None,
    Some("name"),
    Some(s"""ellipsis.success("$actionResult");"""),
    Some("{successResult}"),
    Normal,
    canBeMemoized = false,
    isTest = false,
    OffsetDateTime.now
  )
  def groupFor(team: Team): BehaviorGroup = BehaviorGroup(
    IDs.next,
    None,
    team,
    OffsetDateTime.now
  )
  def groupVersionFor(team: Team): BehaviorGroupVersion = BehaviorGroupVersion(
    IDs.next,
    groupFor(team),
    "A skill",
    None,
    None,
    None,
    OffsetDateTime.now
  )
  val params: Seq[BehaviorParameter] = Seq()
  def teamInfoFor(team: Team) = TeamInfo(
    Seq(),
    Map(),
    None,
    None,
    None
  )
  def ellipsisObjectFor(user: User, team: Team) = {
    val teamInfo = teamInfoFor(team)
    val context = "test"
    EllipsisObject(
      "api",
      IDs.next,
      Map(),
      DeprecatedUserInfo(
        Seq(),
        None,
        user.id,
        Some(context),
        None,
        None,
        None,
        None,
        None,
        None
      ),
      teamInfo,
      teamInfo,
      EventInfo(
        EventUser(
          Seq(),
          user.id,
          Some(context),
          None,
          None,
          None,
          None,
          None,
          None
        ),
        "chat",
        "test",
        "Test",
        None
      )
    )
  }


  "AWSLambdaZipBuilder.build" should {

    "build code that can be run" in new TestContext {

      val functionBody = "ellipsis.success();"
      val apiConfigInfo = ApiConfigInfo(Seq(), Seq(), Seq(), Seq(), Seq())
      val behaviorVersion = behaviorVersionFor(team)

      val invocationJson = AWSLambdaInvocationJsonBuilder(behaviorVersion, ellipsisObjectFor(user, team), Seq()).build
      val builder = AWSLambdaZipBuilder(behaviorVersion.groupVersion, Seq((behaviorVersion, Seq())), Seq(), apiConfigInfo)
      Await.result(builder.build, 20.seconds)
      val code =
        raw"""
           |const handler = require('./index').handler;
           |handler(${invocationJson.toString}, {}, (err, res) => console.log(JSON.stringify(res)));
       """.stripMargin
      val input =  new ByteArrayInputStream(code.getBytes)
      val cwd = new File(builder.dirName)
      val result: String = (Process(Seq("node"), Some(cwd)) #< input).!!
      val jsonResult = Json.parse(result)
      (jsonResult \ "result").as[String] mustBe actionResult

    }

  }

}
