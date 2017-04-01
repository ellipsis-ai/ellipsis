package support

import com.typesafe.config.ConfigFactory
import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import json._
import mocks.MockAWSLambdaService
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import models.behaviors.savedanswer.SavedAnswer
import models.team.Team
import modules.ActorModule
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.cache.CacheApi
import play.api.db.Databases
import play.api.db.evolutions.Evolutions
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import services.{AWSLambdaService, GithubService, PostgresDataService}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait DBSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  lazy val config = ConfigFactory.load()
  lazy val cache = app.injector.instanceOf(classOf[CacheApi])
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])

  def newSavedTeam: Team = runNow(dataService.teams.create(IDs.next))

  def newSavedUserOn(team: Team): User = runNow(dataService.users.createFor(team.id))

  def newSavedAnswerFor(input: Input, user: User): SavedAnswer = {
    runNow(dataService.savedAnswers.ensureFor(input, "answer", user))
  }

  def newInputDataFor(
                       maybeType: Option[BehaviorParameterTypeData] = None,
                       isSavedForTeam: Option[Boolean] = None,
                       isSavedForUser: Option[Boolean] = None
                     ): InputData = {
    InputData(
      Some(IDs.next),
      Some(IDs.next),
      None,
      "param",
      maybeType,
      "",
      isSavedForTeam.exists(identity),
      isSavedForUser.exists(identity)
    )
  }

  def newTriggerData: BehaviorTriggerData = {
    BehaviorTriggerData("foo", false, false, false)
  }

  def newGroupVersionDataFor(group: BehaviorGroup, user: User): BehaviorGroupData = {
    BehaviorGroupData(
      Some(group.id),
      group.team.id,
      name = None,
      description = None,
      icon = None,
      actionInputs = Seq(),
      dataTypeInputs = Seq(),
      behaviorVersions = Seq(),
      requiredOAuth2ApiConfigs = Seq(),
      requiredSimpleTokenApis = Seq(),
      githubUrl = None,
      exportId = None,
      createdAt = None
    )
  }

  def newBehaviorVersionDataFor(behavior: Behavior): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(behavior.team.id, behavior.isDataType, dataService).copy(
      behaviorId = Some(behavior.id),
      isNewBehavior = Some(false)
    )
  }

  def newBehaviorVersionDataFor(group: BehaviorGroup, isDataType: Boolean): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(group.team.id, isDataType, dataService)
  }

  def defaultGroupVersionDataFor(group: BehaviorGroup, user: User): BehaviorGroupData = {
    val input1Data = newInputDataFor()
    val input2Data = newInputDataFor()
    val behaviorVersion1Data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, dataService).copy(
      inputIds = Seq(input1Data.inputId.get)
    )
    val behaviorVersion2Data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, dataService).copy(
      inputIds = Seq(input2Data.inputId.get)
    )
    newGroupVersionDataFor(group, user).copy(
      behaviorVersions = Seq(behaviorVersion1Data, behaviorVersion2Data),
      actionInputs = Seq(input1Data, input2Data)
    )
  }

  def newSavedGroupVersionFor(group: BehaviorGroup, user: User, maybeData: Option[BehaviorGroupData] = None): BehaviorGroupVersion = {
    val data = maybeData.getOrElse(defaultGroupVersionDataFor(group, user))
    runNow(dataService.behaviorGroupVersions.createFor(group, user, data.copyForNewVersionOf(group)))
  }

  def behaviorVersionFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): BehaviorVersion = {
    runNow(dataService.behaviorVersions.findFor(behavior, groupVersion)).get
  }

  def newSavedBehaviorGroupFor(team: Team): BehaviorGroup = {
    runNow(dataService.behaviorGroups.createFor(None, team))
  }

  def withEmptyDB[T](dataService: PostgresDataService, fn: PostgresDatabase => T) = {
    Databases.withDatabase(
      driver = config.getString("db.default.driver"),
      url = config.getString("db.default.url"),
      config = Map(
        "username" -> config.getString("db.default.username"),
        "password" -> config.getString("db.default.password")
      )
    ) { database =>
      Evolutions.withEvolutions(database) {
        try {
          fn(dataService.models.db)
        } finally {
          // Misguided legacy down evolutions will blow up if any of these exist, so delete them
          runNow(dataService.slackProfiles.deleteAll())
          runNow(dataService.collectedParameterValues.deleteAll())
          runNow(dataService.conversations.deleteAll())
        }
      }
    }
  }

  def runNow[T](future: Future[T]): T = {
    Await.result(future, 30.seconds)
  }
}
