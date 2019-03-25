package support

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json._
import mocks.{MockAWSLambdaService, MockCacheService, MockSlackEventService}
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.oauth2api.{AuthorizationCode, OAuth2Api}
import models.accounts.oauth2application.OAuth2Application
import models.accounts.slack.SlackProvider
import models.accounts.user.User
import models.behaviors.BotResultService
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupdeployment.BehaviorGroupDeployment
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorgroupversionsha.BehaviorGroupVersionSHA
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.input.Input
import models.behaviors.savedanswer.SavedAnswer
import models.behaviors.triggers.MessageSent
import models.team.Team
import modules.ActorModule
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration}
import services._
import services.caching.CacheService
import services.slack.{SlackApiService, SlackEventService}
import slick.dbio.DBIO

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait DBSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  lazy val config = app.injector.instanceOf(classOf[Configuration])
  lazy val dbApi = app.injector.instanceOf(classOf[DBApi])
  lazy val cacheService = app.injector.instanceOf(classOf[CacheService])
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])
  lazy val services = app.injector.instanceOf(classOf[DefaultServices])

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      overrides(bind[SlackEventService].to[MockSlackEventService]).
      overrides(bind[CacheService].to[MockCacheService]).
      overrides(bind[SlackApiService].toInstance(mock[SlackApiService])).
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])
  val lambdaService = app.injector.instanceOf(classOf[AWSLambdaService])
  implicit val actorSystem = app.injector.instanceOf(classOf[ActorSystem])
  val slackEventService = app.injector.instanceOf(classOf[SlackEventService])
  val slackApiService = app.injector.instanceOf(classOf[SlackApiService])
  val ws = app.injector.instanceOf(classOf[WSClient])
  val botResultService = app.injector.instanceOf(classOf[BotResultService])
  implicit val ec: ExecutionContext = app.injector.instanceOf(classOf[ExecutionContext])


  def newSavedTeam: Team = {
    runNow {
      for {
        label <- Future.successful(scala.util.Random.nextInt(10000))
        org <- dataService.organizations.create(s"o-${label}")
        team <- dataService.teams.create(s"t-${label}", org)
      } yield {
        team
      }
    }
  }

  def newSavedUserOn(team: Team): User = runNow(dataService.users.createFor(team.id))

  def newSavedLinkedAccountFor(user: User, slackUserId: String): LinkedAccount = {
    val account = LinkedAccount(user, LoginInfo(SlackProvider.ID, slackUserId), OffsetDateTime.now)
    runNow(dataService.linkedAccounts.save(account))
  }

  def newSavedLinkedAccountFor(user: User): LinkedAccount = {
    newSavedLinkedAccountFor(user, IDs.next)
  }

  def newSavedLinkedAccount: LinkedAccount = {
    newSavedLinkedAccountFor(newSavedUserOn(newSavedTeam))
  }

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
    BehaviorTriggerData("foo", false, false, false, MessageSent.toString)
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
      requiredAWSConfigs = Seq(),
      requiredOAuthApiConfigs = Seq(),
      requiredSimpleTokenApis = Seq(),
      gitSHA = None,
      exportId = None,
      createdAt = None,
      author = None,
      deployment = None,
      metaData = None,
      isManaged = false,
      managedContact = None,
      linkedGithubRepo = None
    )
  }

  def newBehaviorVersionDataFor(behavior: Behavior): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(behavior.team.id, behavior.isDataType, isTest = false, None, dataService).copy(
      behaviorId = Some(behavior.id),
      isNew = Some(false)
    )
  }

  def newBehaviorVersionDataFor(group: BehaviorGroup, isDataType: Boolean): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(group.team.id, isDataType, isTest = false, None, dataService)
  }

  def defaultGroupVersionDataFor(group: BehaviorGroup, user: User): BehaviorGroupData = {
    val input1Data = newInputDataFor()
    val input2Data = newInputDataFor()
    val behaviorVersion1Data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, isTest = false, maybeName = None, dataService).copy(
      inputIds = Seq(input1Data.inputId.get)
    )
    val behaviorVersion2Data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, isTest = false, maybeName = None, dataService).copy(
      inputIds = Seq(input2Data.inputId.get)
    )
    newGroupVersionDataFor(group, user).copy(
      actionInputs = Seq(input1Data, input2Data),
      behaviorVersions = Seq(behaviorVersion1Data, behaviorVersion2Data)
    )
  }

  def newSavedGroupVersionFor(group: BehaviorGroup, user: User, maybeData: Option[BehaviorGroupData] = None): BehaviorGroupVersion = {
    val data = maybeData.getOrElse(defaultGroupVersionDataFor(group, user))
    runNow(dataService.behaviorGroupVersions.createForBehaviorGroupData(group, user, data.copyForNewVersionOf(group), forceNode6 = false))
  }

  def newSavedDeploymentFor(groupVersion: BehaviorGroupVersion, user: User): BehaviorGroupDeployment = {
    runNow(dataService.behaviorGroupDeployments.deploy(groupVersion, user.id, None))
  }

  def newSavedSHAFor(groupVersion: BehaviorGroupVersion): BehaviorGroupVersionSHA = {
    runNow(dataService.behaviorGroupVersionSHAs.createForAction(groupVersion, "made-up-ABCDEFGHIJ"))
  }

  def behaviorVersionFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): BehaviorVersion = {
    runNow(dataService.behaviorVersions.findFor(behavior, groupVersion)).get
  }

  def newSavedBehaviorVersionFor(behavior: Behavior, groupVersion: BehaviorGroupVersion, user: User): BehaviorVersion = {
    runNow(dataService.behaviorVersions.createForAction(behavior, groupVersion, ApiConfigInfo(Seq(), Seq(), Seq(), Seq(), Seq()), Some(user), newBehaviorVersionDataFor(behavior)))
  }

  def newSavedBehaviorGroupFor(team: Team): BehaviorGroup = {
    runNow(dataService.behaviorGroups.createFor(None, team))
  }

  def newSavedBehaviorFor(group: BehaviorGroup): Behavior = {
    runNow(dataService.behaviors.createForAction(group, None, None, isDataType = false))
  }

  def newSavedOAuth2Api: OAuth2Api = {
    runNow(dataService.oauth2Apis.save(OAuth2Api(IDs.next, IDs.next, AuthorizationCode, Some(""), "", None, None, None)))
  }

  def newSavedOAuth2ApplicationFor(api: OAuth2Api, team: Team): OAuth2Application = {
    runNow(dataService.oauth2Applications.save(OAuth2Application(IDs.next, IDs.next, api, IDs.next, IDs.next, None, team.id, isShared = false)))
  }

  def newSavedRequiredOAuth2ConfigFor(api: OAuth2Api, groupVersion: BehaviorGroupVersion): RequiredOAuth2ApiConfig = {
    val data = RequiredOAuthApiConfigData(None, None, api.id, None, "default", None)
    runNow(dataService.requiredOAuth2ApiConfigs.maybeCreateFor(data, groupVersion)).get
  }

  def newSavedRequiredAWSConfigFor(name: String, groupVersion: BehaviorGroupVersion): RequiredAWSConfig = {
    val data = RequiredAWSConfigData(None, None, name, None)
    runNow(dataService.requiredAWSConfigs.createForAction(data, groupVersion))
  }

  def withEmptyDB[T](dataService: PostgresDataService, fn: () => T) = {
    val database = dbApi.database("default")

    Evolutions.withEvolutions(database) {
      try {
        fn()
      } finally {
        runNow(dataService.collectedParameterValues.deleteAll())
        runNow(dataService.conversations.deleteAll())
        runNow(dataService.behaviorGroupVersionSHAs.deleteAll())
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
