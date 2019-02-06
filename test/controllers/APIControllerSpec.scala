package controllers

import java.time.{LocalTime, OffsetDateTime}

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import controllers.api.APIController
import json.Formatting._
import json.{APIErrorData, APIResultWithErrorsData, APITokenData}
import models.IDs
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.apitoken.APIToken
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.events._
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.recurrence.Daily
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.testing.TestMessageEvent
import models.behaviors.{BotResult, BotResultService, SimpleTextResult}
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataService
import services.caching.CacheService
import services.slack.apiModels.Attachment
import services.slack.{SlackApiClient, SlackEventService}
import support.ControllerTestContext
import utils.{SlackChannels, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

class APIControllerSpec extends PlaySpec with MockitoSugar {

  case class APITestData(
                          token: String,
                          event: Event
                        )

  def setUpMocksForSlack(
                     team: Team,
                     user: User,
                     isTokenValid: Boolean,
                     maybeTokenBehaviorId: Option[String],
                     app: Application,
                     eventHandler: EventHandler,
                     dataService: DataService,
                     cacheService: CacheService,
                     slackEventService: SlackEventService,
                     botResultService: BotResultService,
                     now: OffsetDateTime = OffsetDateTime.now
                   )(implicit ec: ExecutionContext) = {
    val token = IDs.next
    when(dataService.apiTokens.find(token)).thenReturn(Future.successful(None))
    val behaviorId = maybeTokenBehaviorId.getOrElse(IDs.next)
    val maybeInvocationToken = if (isTokenValid) {
      Some(InvocationToken(token, defaultSlackUserId, behaviorId, None, Some(defaultSlackTeamId), now))
    } else {
      None
    }
    val maybeUserForToken = maybeInvocationToken.map(_ => user)
    when(dataService.invocationTokens.findNotExpired(token)).thenReturn(Future.successful(maybeInvocationToken))
    when(dataService.users.findForInvocationToken(token)).thenReturn(Future.successful(maybeUserForToken))
    when(dataService.teams.find(user.teamId)).thenReturn(Future.successful(Some(team)))
    when(dataService.apiTokens.maybeUserForApiToken(token)).thenReturn(Future.successful(maybeUserForToken))
    val botProfile = SlackBotProfile(defaultChannel, team.id, defaultSlackTeamId, defaultSlackToken, OffsetDateTime.now, allowShortcutMention = true)

    val apiController = app.injector.instanceOf(classOf[APIController])
    implicit val actorSystem = apiController.actorSystem

    val mockSlackClient = mock[SlackApiClient]
    when(slackEventService.clientFor(botProfile)).thenReturn(mockSlackClient)
    when(mockSlackClient.postChatMessage(anyString, anyString, any[Option[String]], any[Option[Boolean]], any[Option[String]],
      any[Option[String]], any[Option[Seq[Attachment]]], any[Option[Boolean]], any[Option[Boolean]],
      any[Option[String]], any[Option[String]], any[Option[Boolean]], any[Option[Boolean]],
      any[Option[String]], any[Option[Boolean]])).thenReturn(Future.successful(SlackTimestamp.now))

    val event = SlackMessageEvent(
      SlackEventContext(
        botProfile,
        defaultChannel,
        None,
        defaultSlackUserId
      ),
      SlackMessage.fromUnformattedText("foo", botProfile),
      None,
      SlackTimestamp.now,
      Some(EventType.api),
      isUninterruptedConversation = false,
      isEphemeral = false,
      maybeResponseUrl = None,
      beQuiet = false
    )
    when(dataService.slackBotProfiles.allForSlackTeamId(defaultSlackTeamId)).thenReturn(Future.successful(Seq(botProfile)))
    val loginInfo = LoginInfo(defaultContext, defaultSlackUserId)
    val slackProfile = SlackProfile(SlackUserTeamIds(defaultSlackTeamId), loginInfo, None)
    when(dataService.users.maybeSlackProfileFor(user)).thenReturn(Future.successful(Some(slackProfile)))
    val mockSlackChannels = mock[SlackChannels]
    when(dataService.slackBotProfiles.channelsFor(any[SlackBotProfile])).thenReturn(mockSlackChannels)
    when(mockSlackChannels.maybeIdFor(defaultChannel)).thenReturn(Future.successful(Some(defaultChannel)))

    when(dataService.conversations.allOngoingFor(event.eventContext, None)).thenReturn(Future.successful(Seq()))
    when(eventHandler.handle(any[Event], org.mockito.Matchers.eq(None))).thenReturn(Future.successful(Seq(SimpleTextResult(event, None, "result", responseType = Normal))))

    when(botResultService.sendIn(any[BotResult], any[Option[Boolean]])(any[ActorSystem])).thenReturn(Future.successful(Some(SlackTimestamp.now)))

    token
  }

  def setupMocksForNoMedium(
                             team: Team,
                             user: User,
                             isTokenValid: Boolean,
                             maybeTokenBehaviorId: Option[String],
                             app: Application,
                             eventHandler: EventHandler,
                             dataService: DataService,
                             cacheService: CacheService,
                             botResultService: BotResultService,
                             now: OffsetDateTime = OffsetDateTime.now
                           )(implicit ec: ExecutionContext) = {
    val token = IDs.next
    when(dataService.apiTokens.find(token)).thenReturn(Future.successful(None))
    val behaviorId = maybeTokenBehaviorId.getOrElse(IDs.next)
    val maybeInvocationToken = if (isTokenValid) {
      Some(InvocationToken(token, user.id, behaviorId, None, Some(team.id), now))
    } else {
      None
    }
    val maybeUserForToken = maybeInvocationToken.map(_ => user)
    when(dataService.invocationTokens.findNotExpired(token)).thenReturn(Future.successful(maybeInvocationToken))
    when(dataService.users.findForInvocationToken(token)).thenReturn(Future.successful(maybeUserForToken))
    when(dataService.teams.find(user.teamId)).thenReturn(Future.successful(Some(team)))
    when(dataService.apiTokens.maybeUserForApiToken(token)).thenReturn(Future.successful(maybeUserForToken))
    val apiController = app.injector.instanceOf(classOf[APIController])
    implicit val actorSystem = apiController.actorSystem

    val event = TestMessageEvent(
      TestEventContext(user, team),
      "foo",
      includesBotMention = true
    )
    when(dataService.slackBotProfiles.allForSlackTeamId(any[String])).thenReturn(Future.successful(Seq()))
    val loginInfo = LoginInfo(defaultContext, defaultSlackUserId)
    val slackProfile = SlackProfile(SlackUserTeamIds(defaultSlackTeamId), loginInfo, None)
    when(dataService.users.maybeSlackProfileFor(user)).thenReturn(Future.successful(Some(slackProfile)))
    when(dataService.users.maybeMSTeamsProfileFor(user)).thenReturn(Future.successful(None))
    when(dataService.msTeamsBotProfiles.find(any[String])).thenReturn(Future.successful(None))

    when(dataService.conversations.allOngoingFor(event.eventContext, None)).thenReturn(Future.successful(Seq()))
    when(eventHandler.handle(any[Event], org.mockito.Matchers.eq(None))).thenReturn(Future.successful(Seq(SimpleTextResult(event, None, "result", responseType = Normal))))

    when(botResultService.sendIn(any[BotResult], any[Option[Boolean]])(any[ActorSystem])).thenReturn(Future.successful(None))

    token
  }

  val defaultContext = "slack"
  val defaultChannel = "C1234567"
  val defaultSlackTeamId = "T1234567"
  val defaultSlackUserId = "U1234567"
  val defaultSlackToken = IDs.next

  def postMessageBodyFor(
                          message: String,
                          channel: String,
                          token: String
                        ): JsValue = {
    JsObject(Seq(
      ("message", JsString(message)),
      ("responseContext", JsString(defaultContext)),
      ("channel", JsString(channel)),
      ("token", JsString(token))
    ))
  }

  def maybeErrorFrom(jsResult: JsValue): Option[APIErrorData] = {
    jsResult.validate[APIResultWithErrorsData] match {
      case JsSuccess(data, jsPath) => data.errors.headOption
      case JsError(e) => None
    }
  }

  "postMessage" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.api.routes.APIController.postMessage()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid token", Some("token")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = true, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.api.routes.APIController.postMessage()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[String]] match {
          case JsSuccess(data, jsPath) => {
            data must have length 1
            data.head mustBe "result"
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }

  }

  def runActionBodyFor(
                        maybeActionName: Option[String],
                        maybeTrigger: Option[String],
                        maybeChannel: Option[String],
                        token: String,
                        maybeResponseContext: Option[String] = None
                      ): JsValue = {
    var elements = Seq(
      ("responseContext", JsString(maybeResponseContext.getOrElse(defaultContext))),
      ("channel", maybeChannel.map(JsString(_)).getOrElse(JsNull)),
      ("token", JsString(token))
    )
    maybeActionName.foreach { actionName =>
      elements = elements ++ Seq(("actionName", JsString(actionName)))
    }
    maybeTrigger.foreach { trigger =>
      elements = elements ++ Seq(("trigger", JsString(trigger)))
    }
    JsObject(elements)
  }


  "runAction" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = runActionBodyFor(Some("foo"), None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid token", Some("token")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "400 when neither actionName nor trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)

        val body = runActionBodyFor(None, None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("One and only one of actionName and trigger must be set", None))
      }
    }

    "400 when both actionName and trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)

        val body = runActionBodyFor(Some("foo"), Some("bar"), Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("One and only one of actionName and trigger must be set", None))
      }
    }

    "400 when channel is missing for Slack" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)

        val body = runActionBodyFor(Some("foo"), None, None, token)
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("To run actions for Slack, `channel` must be set", Some("channel")))
      }
    }

    "respond with a valid result for actionName" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"
        when(dataService.behaviorVersions.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
        when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))

        val body = runActionBodyFor(Some(actionName), None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[String]] match {
          case JsSuccess(data, jsPath) => {
            data must have length 1
            data.head mustBe "result"
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }

    "respond with a valid result for actionName without channel for NoMediumApiContext" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val token = setupMocksForNoMedium(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, botResultService)
        val actionName = "foo"
        when(dataService.behaviorVersions.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
        when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))

        val body = runActionBodyFor(Some(actionName), None, Some(defaultChannel), token, Some("test"))
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[String]] match {
          case JsSuccess(data, jsPath) => {
            data must have length 1
            data.head mustBe "result"
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }

    "respond with a valid result for trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val trigger = "foo"

        val body = runActionBodyFor(None, Some(trigger), Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[String]] match {
          case JsSuccess(data, jsPath) => {
            data must have length 1
            data.head mustBe "result"
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }


  }

  "say" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.api.routes.APIController.say()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid token", Some("token")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = true, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val message = "foo"
        val body = postMessageBodyFor(message, defaultChannel, token)
        val request = FakeRequest(controllers.api.routes.APIController.say()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK
        val resultJson = contentAsJson(result)
        resultJson.validate[Seq[String]] match {
          case JsSuccess(data, jsPath) => {
            data must have length 1
            data.head mustBe message
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }
  }

  def scheduleActionBodyFor(
                            maybeActionName: Option[String],
                            maybeTrigger: Option[String],
                            channel: String,
                            recurrenceString: String,
                            token: String
                          ): JsValue = {
    var elements = Seq(
      ("responseContext", JsString(defaultContext)),
      ("channel", JsString(channel)),
      ("recurrence", JsString(recurrenceString)),
      ("token", JsString(token))
    )
    maybeActionName.foreach { actionName =>
      elements = elements ++ Seq(("actionName", JsString(actionName)))
    }
    maybeTrigger.foreach { trigger =>
      elements = elements ++ Seq(("trigger", JsString(trigger)))
    }
    JsObject(elements)
  }

  "scheduleAction" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = scheduleActionBodyFor(Some("foo"), None, defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.api.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid token", Some("token")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "400 when neither actionName nor trigger is supplied" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = scheduleActionBodyFor(None, None, defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.api.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("One and only one of actionName and trigger must be set", None))
      }
    }

    "400 when both actionName and trigger are supplied" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = scheduleActionBodyFor(Some("foo"), Some("bar"), defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.api.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("One and only one of actionName and trigger must be set", None))
      }
    }

    "respond with a valid result for actionName" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"

        val recurrenceString = "every day at noon"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
        when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))
        when(dataService.users.ensureUserFor(any[LoginInfo], any[Seq[LoginInfo]], anyString)).thenReturn(Future.successful(user))
        when(dataService.behaviors.maybeCurrentVersionFor(behavior)).thenReturn(Future.successful(Some(targetBehaviorVersion)))
        when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(Future.successful(Some(groupVersion)))
        when(dataService.scheduledBehaviors.maybeCreateWithRecurrenceText(behavior, Map(), recurrenceString, user, team, Some(defaultChannel), false)).thenReturn {
          Future.successful(
            Some(
              ScheduledBehavior(
                IDs.next,
                behavior,
                Map(),
                Some(user),
                team,
                Some(defaultChannel),
                isForIndividualMembers = false,
                Daily(IDs.next, 1, 0, None, LocalTime.of(12, 0, 0), team.timeZone),
                OffsetDateTime.now,
                OffsetDateTime.now
              )
            )
          )
        }

        val body = scheduleActionBodyFor(Some(actionName), None, defaultChannel, recurrenceString, token)
        val request = FakeRequest(controllers.api.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, times(1)).maybeCreateWithRecurrenceText(behavior, Map(), recurrenceString, user, team, Some(defaultChannel), false)
      }
    }

    "respond with a valid result for trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val trigger = "foo"
        val recurrenceString = "every day at noon"
        when(dataService.users.ensureUserFor(any[LoginInfo], any[Seq[LoginInfo]], anyString)).thenReturn(Future.successful(user))
        when(dataService.scheduledMessages.maybeCreateWithRecurrenceText(trigger, recurrenceString, user, team, Some(defaultChannel), false)).thenReturn {
          Future.successful(
            Some(
              ScheduledMessage(
                IDs.next,
                trigger,
                Some(user),
                team,
                Some(defaultChannel),
                isForIndividualMembers = false,
                Daily(IDs.next, 1, 0, None, LocalTime.of(12, 0, 0), team.timeZone),
                OffsetDateTime.now,
                OffsetDateTime.now
              )
            )
          )
        }

        val body = scheduleActionBodyFor(None, Some(trigger), defaultChannel, recurrenceString, token)
        val request = FakeRequest(controllers.api.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledMessages, times(1)).maybeCreateWithRecurrenceText(trigger, recurrenceString, user, team, Some(defaultChannel), false)
      }
    }
  }

  def unscheduleActionBodyFor(
                             maybeActionName: Option[String],
                             maybeTrigger: Option[String],
                             maybeUserId: Option[String],
                             maybeChannel: Option[String],
                             token: String
                           ): JsValue = {
    var elements = Seq(
      ("token", JsString(token))
    )
    maybeActionName.foreach { actionName =>
      elements = elements ++ Seq(("actionName", JsString(actionName)))
    }
    maybeTrigger.foreach { trigger =>
      elements = elements ++ Seq(("trigger", JsString(trigger)))
    }
    maybeUserId.foreach { uid =>
      elements = elements ++ Seq(("userId", JsString(uid)))
    }
    maybeChannel.foreach { channel =>
      elements = elements ++ Seq(("channel", JsString(channel)))
    }
    JsObject(elements)
  }

  "unscheduleAction" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = unscheduleActionBodyFor(Some("foo"), None, None, None, token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid token", Some("token")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "400 for neither actionName nor trigger" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = unscheduleActionBodyFor(None, None, None, None, token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("One and only one of actionName and trigger must be set", None))
      }
    }

    "400 for both actionName and trigger" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = unscheduleActionBodyFor(Some("foo"), Some("bar"), None, None, token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("One and only one of actionName and trigger must be set", None))
      }
    }

    "404 for invalid user id" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"

        when(dataService.behaviorVersions.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
        when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))
        val invalidUserId = "invalid"
        when(dataService.users.find(invalidUserId)).thenReturn(Future.successful(None))
        val body = unscheduleActionBodyFor(Some("foo"), None, Some(invalidUserId), None, token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData(s"Couldn't find a user with ID `${invalidUserId}`", Some("userId")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "404 for invalid action name" in new ControllerTestContext {
      val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
      val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
      val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
      val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
      val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
      val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
      val actionName = "foo"

      when(dataService.behaviorVersions.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
      when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
      when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(None))
      when(dataService.users.ensureUserFor(any[LoginInfo], any[Seq[LoginInfo]], anyString)).thenReturn(Future.successful(user))
      val body = unscheduleActionBodyFor(Some(actionName), None, None, Some(defaultChannel), token)
      val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData(s"Couldn't find an action with name `$actionName`", Some("actionName")))
    }

    "respond with a valid result for a scheduled actionName" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"

        when(dataService.behaviorVersions.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
        when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))
        when(dataService.users.ensureUserFor(any[LoginInfo], any[Seq[LoginInfo]], anyString)).thenReturn(Future.successful(user))
        when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(Future.successful(Some(groupVersion)))
        val scheduledBehavior = ScheduledBehavior(
          IDs.next,
          behavior,
          Map(),
          Some(user),
          team,
          Some(defaultChannel),
          isForIndividualMembers = false,
          Daily(IDs.next, 1, 0, None, LocalTime.of(12, 0, 0), team.timeZone),
          OffsetDateTime.now,
          OffsetDateTime.now
        )
        when(dataService.scheduledBehaviors.allForBehavior(behavior, None, Some(defaultChannel))).thenReturn(Future.successful(Seq(scheduledBehavior)))
        when(dataService.scheduledBehaviors.delete(scheduledBehavior)).thenReturn(Future.successful(Some(scheduledBehavior)))

        val body = unscheduleActionBodyFor(Some(actionName), None, None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, times(1)).delete(scheduledBehavior)
      }
    }

    "respond with a valid result for a valid actionName that isn't scheduled" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val groupVersion = BehaviorGroupVersion(IDs.next, group, "skill", None, None, None, OffsetDateTime.now)
        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val originatingBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val targetBehaviorVersion = BehaviorVersion(IDs.next, behavior, groupVersion, None, None, None, None, Normal, false, false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehaviorVersion.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"

        when(dataService.behaviorVersions.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviorVersions.findWithoutAccessCheck(originatingBehaviorVersion.id)).thenReturn(Future.successful(Some(originatingBehaviorVersion)))
        when(dataService.behaviorVersions.findByName(actionName, groupVersion)).thenReturn(Future.successful(Some(targetBehaviorVersion)))
        when(dataService.users.ensureUserFor(any[LoginInfo], any[Seq[LoginInfo]], anyString)).thenReturn(Future.successful(user))
        when(dataService.scheduledBehaviors.allForBehavior(behavior, None, Some(defaultChannel))).thenReturn(Future.successful(Seq()))
        when(dataService.scheduledBehaviors.delete(any[ScheduledBehavior])).thenReturn(Future.successful(None))

        val body = unscheduleActionBodyFor(Some(actionName), None, None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, never()).delete(any[ScheduledBehavior])
      }
    }

    "respond with a valid result for trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val trigger = "foo"
        when(dataService.users.ensureUserFor(any[LoginInfo], any[Seq[LoginInfo]], anyString)).thenReturn(Future.successful(user))
        val scheduledMessage = ScheduledMessage(
          IDs.next,
          trigger,
          Some(user),
          team,
          Some(defaultChannel),
          isForIndividualMembers = false,
          Daily(IDs.next, 1, 0, None, LocalTime.of(12, 0, 0), team.timeZone),
          OffsetDateTime.now,
          OffsetDateTime.now
        )

        when(dataService.scheduledMessages.allForText(trigger, team, None, Some(defaultChannel))).thenReturn(Future.successful(Seq(scheduledMessage)))
        when(dataService.scheduledMessages.delete(scheduledMessage)).thenReturn(Future.successful(Some(scheduledMessage)))

        val body = unscheduleActionBodyFor(None, Some(trigger), None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.api.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledMessages, times(1)).allForText(trigger, team, None, Some(defaultChannel))
        verify(dataService.scheduledMessages, times(1)).delete(scheduledMessage)
      }
    }

  }

  "generateApiToken" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksForSlack(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = JsObject(Seq(
          ("token", JsString(token))
        ))
        val request = FakeRequest(controllers.api.routes.APIController.generateApiToken()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        maybeErrorFrom(contentAsJson(result)) mustEqual Some(APIErrorData("Invalid token", Some("token")))
        verify(dataService.apiTokens, times(1)).maybeUserForApiToken(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val tokenBehaviorId = IDs.next
        val now = OffsetDateTime.now
        val token = setUpMocksForSlack(team, user, isTokenValid = true, Some(tokenBehaviorId), app, eventHandler, dataService, cacheService, slackEventService, botResultService, now)
        val invocationToken = InvocationToken(token, defaultSlackUserId, tokenBehaviorId, None, Some(defaultSlackTeamId), now)
        val newToken = APIToken(IDs.next, IDs.next, user.id, None, isOneTime = false, isRevoked = false, None, OffsetDateTime.now)
        when(dataService.apiTokens.createFor(invocationToken, None, false)).thenReturn(Future.successful(newToken))
        val body = JsObject(Seq(
          ("token", JsString(token))
        ))
        val request = FakeRequest(controllers.api.routes.APIController.generateApiToken()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK
        val resultJson = contentAsJson(result)
        resultJson.validate[APITokenData] match {
          case JsSuccess(data, jsPath) => {
            data.isOneTime mustBe false
            data.maybeExpirySeconds.isDefined mustBe false
          }
          case JsError(e) => {
            assert(false, "Result didn't validate")
          }
        }
      }
    }
  }


}
