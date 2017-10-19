package controllers

import java.time.{LocalTime, OffsetDateTime}

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.{BotResult, BotResultService, SimpleTextResult}
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventHandler, SlackMessage, SlackMessageEvent}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.recurrence.Daily
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CacheService, DataService, SlackEventService}
import slack.api.SlackApiClient
import slack.models.Attachment
import support.ControllerTestContext
import utils.{SlackChannels, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

class APIControllerSpec extends PlaySpec with MockitoSugar {

  case class APITestData(
                          token: String,
                          event: Event
                        )

  def setUpMocksFor(
                     team: Team,
                     user: User,
                     isTokenValid: Boolean,
                     maybeTokenBehaviorId: Option[String],
                     app: Application,
                     eventHandler: EventHandler,
                     dataService: DataService,
                     cacheService: CacheService,
                     slackEventService: SlackEventService,
                     botResultService: BotResultService
                   )(implicit ec: ExecutionContext) = {
    val token = IDs.next
    when(dataService.apiTokens.find(token)).thenReturn(Future.successful(None))
    val behaviorId = maybeTokenBehaviorId.getOrElse(IDs.next)
    val maybeInvocationToken = if (isTokenValid) {
      Some(InvocationToken(IDs.next, defaultSlackUserId, behaviorId, None, OffsetDateTime.now))
    } else {
      None
    }
    val maybeUserForToken = maybeInvocationToken.map(_ => user)
    when(dataService.invocationTokens.findNotExpired(token)).thenReturn(Future.successful(maybeInvocationToken))
    when(dataService.users.findForInvocationToken(token)).thenReturn(Future.successful(maybeUserForToken))
    when(dataService.teams.find(user.teamId)).thenReturn(Future.successful(Some(team)))
    val botProfile = SlackBotProfile(defaultChannel, team.id, defaultSlackTeamId, defaultSlackToken, OffsetDateTime.now)

    val apiController = app.injector.instanceOf(classOf[APIController])
    implicit val actorSystem = apiController.actorSystem

    val mockSlackClient = mock[SlackApiClient]
    when(slackEventService.clientFor(botProfile)).thenReturn(mockSlackClient)
    when(mockSlackClient.listIms).thenReturn(Future.successful(Seq()))
    when(mockSlackClient.postChatMessage(anyString, anyString, any[Option[String]], any[Option[Boolean]], any[Option[String]],
      any[Option[String]], any[Option[Seq[Attachment]]], any[Option[Boolean]], any[Option[Boolean]],
      any[Option[String]], any[Option[String]], any[Option[Boolean]], any[Option[Boolean]],
      any[Option[String]], any[Option[Boolean]])(any[ActorSystem])).thenReturn(Future.successful(SlackTimestamp.now))
    when(mockSlackClient.listUsers).thenReturn(Future.successful(Seq()))

    val event = SlackMessageEvent(botProfile, defaultChannel, None, defaultSlackUserId, SlackMessage.fromUnformattedText("foo", botProfile.userId), SlackTimestamp.now, mockSlackClient)
    when(dataService.slackBotProfiles.allFor(team)).thenReturn(Future.successful(Seq(botProfile)))
    val loginInfo = LoginInfo(defaultContext, defaultSlackUserId)
    val slackProfile = SlackProfile(defaultSlackTeamId, loginInfo)
    when(dataService.slackProfiles.allFor(team.id)).thenReturn(Future.successful(Seq(slackProfile)))
    val linkedAccount = LinkedAccount(user, loginInfo, OffsetDateTime.now)
    when(dataService.linkedAccounts.maybeForSlackFor(user)).thenReturn(Future.successful(Some(linkedAccount)))
    when(dataService.slackProfiles.find(loginInfo)).thenReturn(Future.successful(Some(slackProfile)))
    val mockSlackChannels = mock[SlackChannels]
    when(dataService.slackBotProfiles.channelsFor(any[SlackBotProfile], any[CacheService])).thenReturn(mockSlackChannels)
    when(mockSlackChannels.maybeIdFor(defaultChannel)).thenReturn(Future.successful(Some(defaultChannel)))

    when(dataService.conversations.allOngoingFor(defaultSlackUserId, event.context, event.maybeChannel, event.maybeThreadId)).thenReturn(Future.successful(Seq()))
    when(eventHandler.handle(any[Event], org.mockito.Matchers.eq(None))).thenReturn(Future.successful(Seq(SimpleTextResult(event, None, "result", forcePrivateResponse = false))))

    when(botResultService.sendIn(
      any[BotResult],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[String]]
    )(any[ActorSystem])
    ).thenReturn(Future.successful(Some(SlackTimestamp.now)))

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

  "postMessage" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.postMessage()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = true, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.postMessage()).withJsonBody(body)
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
                        channel: String,
                        token: String
                      ): JsValue = {
    var elements = Seq(
      ("responseContext", JsString(defaultContext)),
      ("channel", JsString(channel)),
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
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = runActionBodyFor(Some("foo"), None, defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "400 when neither actionName nor trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)

        val body = runActionBodyFor(None, None, defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "One and only one of actionName and trigger must be set"
      }
    }

    "400 when both actionName and trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)

        val body = runActionBodyFor(Some("foo"), Some("bar"), defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "One and only one of actionName and trigger must be set"
      }
    }

    "respond with a valid result for actionName" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))

        val body = runActionBodyFor(Some(actionName), None, defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.runAction()).withJsonBody(body)
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
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val trigger = "foo"

        val body = runActionBodyFor(None, Some(trigger), defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.runAction()).withJsonBody(body)
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
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.say()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result).trim mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = true, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val message = "foo"
        val body = postMessageBodyFor(message, defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.say()).withJsonBody(body)
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
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = scheduleActionBodyFor(Some("foo"), None, defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "400 when neither actionName nor trigger is supplied" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = scheduleActionBodyFor(None, None, defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "One and only one of actionName and trigger must be set"
      }
    }

    "400 when both actionName and trigger are supplied" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = scheduleActionBodyFor(Some("foo"), Some("bar"), defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "One and only one of actionName and trigger must be set"
      }
    }

    "respond with a valid result for actionName" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"
        val recurrenceString = "every day at noon"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))
        when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
        val mockBehaviorVersion = mock[BehaviorVersion]
        when(mockBehaviorVersion.maybeName).thenReturn(Some(actionName))
        when(dataService.behaviors.maybeCurrentVersionFor(targetBehavior)).thenReturn(Future.successful(Some(mockBehaviorVersion)))
        val mockVersion = mock[BehaviorGroupVersion]
        when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(Future.successful(Some(mockVersion)))
        when(dataService.scheduledBehaviors.maybeCreateWithRecurrenceText(targetBehavior, Map(), recurrenceString, user, team, Some(defaultChannel), false)).thenReturn {
          Future.successful(
            Some(
              ScheduledBehavior(
                IDs.next,
                targetBehavior,
                Map(),
                Some(user),
                team,
                Some(defaultChannel),
                isForIndividualMembers = false,
                Daily(IDs.next, 1, LocalTime.of(12, 0, 0), team.timeZone),
                OffsetDateTime.now,
                OffsetDateTime.now
              )
            )
          )
        }

        val body = scheduleActionBodyFor(Some(actionName), None, defaultChannel, recurrenceString, token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, times(1)).maybeCreateWithRecurrenceText(targetBehavior, Map(), recurrenceString, user, team, Some(defaultChannel), false)
      }
    }

    "respond with a valid result for trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val trigger = "foo"
        val recurrenceString = "every day at noon"
        when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
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
                Daily(IDs.next, 1, LocalTime.of(12, 0, 0), team.timeZone),
                OffsetDateTime.now,
                OffsetDateTime.now
              )
            )
          )
        }

        val body = scheduleActionBodyFor(None, Some(trigger), defaultChannel, recurrenceString, token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
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
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = unscheduleActionBodyFor(Some("foo"), None, None, None, token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "400 for neither actionName nor trigger" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = unscheduleActionBodyFor(None, None, None, None, token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "One and only one of actionName and trigger must be set"
      }
    }

    "400 for both actionName and trigger" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val body = unscheduleActionBodyFor(Some("foo"), Some("bar"), None, None, token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "One and only one of actionName and trigger must be set"
      }
    }

    "404 for invalid user id" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))
        val invalidUserId = "invalid"
        when(dataService.users.find(invalidUserId)).thenReturn(Future.successful(None))
        val body = unscheduleActionBodyFor(Some("foo"), None, Some(invalidUserId), None, token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe s"Couldn't find a user with ID `${invalidUserId}`"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "404 for invalid action name" in new ControllerTestContext {
      val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
      val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
      val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
      val actionName = "foo"
      when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
      when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
      when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(None))
      when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
      val body = unscheduleActionBodyFor(Some(actionName), None, None, Some(defaultChannel), token)
      val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe s"Couldn't find an action with name `$actionName`"
    }

    "respond with a valid result for a scheduled actionName" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))
        when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
        val mockVersion = mock[BehaviorGroupVersion]
        when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(Future.successful(Some(mockVersion)))
        val scheduledBehavior = ScheduledBehavior(
          IDs.next,
          targetBehavior,
          Map(),
          Some(user),
          team,
          Some(defaultChannel),
          isForIndividualMembers = false,
          Daily(IDs.next, 1, LocalTime.of(12, 0, 0), team.timeZone),
          OffsetDateTime.now,
          OffsetDateTime.now
        )
        when(dataService.scheduledBehaviors.allForBehavior(targetBehavior, None, Some(defaultChannel))).thenReturn(Future.successful(Seq(scheduledBehavior)))
        when(dataService.scheduledBehaviors.delete(scheduledBehavior)).thenReturn(Future.successful(Some(scheduledBehavior)))

        val body = unscheduleActionBodyFor(Some(actionName), None, None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, times(1)).delete(scheduledBehavior)
      }
    }

    "respond with a valid result for a valid actionName that isn't scheduled" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val actionName = "foo"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))
        when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
        val mockVersion = mock[BehaviorGroupVersion]
        when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(Future.successful(Some(mockVersion)))
        when(dataService.scheduledBehaviors.allForBehavior(targetBehavior, None, Some(defaultChannel))).thenReturn(Future.successful(Seq()))
        when(dataService.scheduledBehaviors.delete(any[ScheduledBehavior])).thenReturn(Future.successful(None))

        val body = unscheduleActionBodyFor(Some(actionName), None, None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, never()).delete(any[ScheduledBehavior])
      }
    }

    "respond with a valid result for trigger" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, None, team, None, isBuiltin = false, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService, cacheService, slackEventService, botResultService)
        val trigger = "foo"
        when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
        val scheduledMessage = ScheduledMessage(
          IDs.next,
          trigger,
          Some(user),
          team,
          Some(defaultChannel),
          isForIndividualMembers = false,
          Daily(IDs.next, 1, LocalTime.of(12, 0, 0), team.timeZone),
          OffsetDateTime.now,
          OffsetDateTime.now
        )

        when(dataService.scheduledMessages.allForText(trigger, team, None, Some(defaultChannel))).thenReturn(Future.successful(Seq(scheduledMessage)))
        when(dataService.scheduledMessages.delete(scheduledMessage)).thenReturn(Future.successful(Some(scheduledMessage)))

        val body = unscheduleActionBodyFor(None, Some(trigger), None, Some(defaultChannel), token)
        val request = FakeRequest(controllers.routes.APIController.unscheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledMessages, times(1)).allForText(trigger, team, None, Some(defaultChannel))
        verify(dataService.scheduledMessages, times(1)).delete(scheduledMessage)
      }
    }

  }


}
