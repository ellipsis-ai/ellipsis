package support

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import json._
import mocks.MockAWSLambdaService
import models.IDs
import models.accounts.oauth2api.{AuthorizationCode, OAuth2Api}
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.behaviors.BotResultService
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.input.Input
import models.behaviors.savedanswer.SavedAnswer
import models.team.Team
import modules.ActorModule
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.db.evolutions.Evolutions
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration}
import services._
import slick.dbio.DBIO

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait DBSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  lazy val config = ConfigFactory.load()
  lazy val cacheService = app.injector.instanceOf(classOf[CacheService])
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])
  lazy val services = app.injector.instanceOf(classOf[DefaultServices])

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      overrides(bind[SlackEventService].toInstance(mock[SlackEventService])).
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])
  val lambdaService = app.injector.instanceOf(classOf[AWSLambdaService])
  val actorSystem = app.injector.instanceOf(classOf[ActorSystem])
  val slackEventService = app.injector.instanceOf(classOf[SlackEventService])
  val ws = app.injector.instanceOf(classOf[WSClient])
  val botResultService = app.injector.instanceOf(classOf[BotResultService])

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
      libraryVersions = Seq(),
      nodeModuleVersions = Seq(),
      requiredOAuth2ApiConfigs = Seq(),
      requiredSimpleTokenApis = Seq(),
      githubUrl = None,
      exportId = None,
      createdAt = None
    )
  }

  def newBehaviorVersionDataFor(behavior: Behavior): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(behavior.team.id, behavior.isDataType, None, dataService).copy(
      behaviorId = Some(behavior.id),
      isNew = Some(false)
    )
  }

  def newBehaviorVersionDataFor(group: BehaviorGroup, isDataType: Boolean): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(group.team.id, isDataType, None, dataService)
  }

  def defaultGroupVersionDataFor(group: BehaviorGroup, user: User): BehaviorGroupData = {
    val input1Data = newInputDataFor()
    val input2Data = newInputDataFor()
    val behaviorVersion1Data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, maybeName = None, dataService).copy(
      inputIds = Seq(input1Data.inputId.get)
    )
    val behaviorVersion2Data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, maybeName = None, dataService).copy(
      inputIds = Seq(input2Data.inputId.get)
    )
    newGroupVersionDataFor(group, user).copy(
      behaviorVersions = Seq(behaviorVersion1Data, behaviorVersion2Data),
      actionInputs = Seq(input1Data, input2Data)
    )
  }

  def newSavedGroupVersionFor(group: BehaviorGroup, user: User, maybeData: Option[BehaviorGroupData] = None): BehaviorGroupVersion = {
    val data = maybeData.getOrElse(defaultGroupVersionDataFor(group, user))
    runNow(dataService.behaviorGroupVersions.createFor(group, user, data.copyForNewVersionOf(group), forceNodeModuleUpdate = false))
  }

  def behaviorVersionFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): BehaviorVersion = {
    runNow(dataService.behaviorVersions.findFor(behavior, groupVersion)).get
  }

  def newSavedBehaviorGroupFor(team: Team): BehaviorGroup = {
    runNow(dataService.behaviorGroups.createFor(None, team))
  }

  def newSavedOAuth2Api: OAuth2Api = {
    runNow(dataService.oauth2Apis.save(OAuth2Api(IDs.next, IDs.next, AuthorizationCode, Some(""), "", None, None, None)))
  }

  def newSavedOAuth2ApplicationFor(api: OAuth2Api, team: Team): OAuth2Application = {
    runNow(dataService.oauth2Applications.save(OAuth2Application(IDs.next, IDs.next, api, IDs.next, IDs.next, None, team.id, isShared = false)))
  }

  def newSavedRequiredOAuth2ConfigFor(api: OAuth2Api, groupVersion: BehaviorGroupVersion): RequiredOAuth2ApiConfig = {
    val data = RequiredOAuth2ApiConfigData(None, api.id, None, None)
    runNow(dataService.requiredOAuth2ApiConfigs.maybeCreateFor(data, groupVersion)).get
  }

  def withEmptyDB[T](dataService: PostgresDataService, fn: () => T) = {
    Databases.withDatabase(
      driver = config.getOptional[String]("slick.dbs.default.driver"),
      url = config.getOptional[String]("slick.dbs.default.url"),
      config = Map(
        "username" -> config.getOptional[String]("slick.dbs.default.username"),
        "password" -> config.getOptional[String]("slick.dbs.default.password")
      )
    ) { database =>
      Evolutions.withEvolutions(database) {
        try {
          fn()
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

  def runNow[T](dbio: DBIO[T]): T = {
    runNow(dataService.run(dbio))
  }
}
