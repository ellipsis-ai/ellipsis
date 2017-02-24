package controllers

import java.time.{LocalTime, OffsetDateTime}

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
import models.accounts.user.User
import models.behaviors.SimpleTextResult
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventHandler, SlackMessageEvent}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.recurrence.Daily
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataService
import slack.api.SlackApiClient
import slack.models.Attachment
import support.ControllerTestContext
import utils.{SlackChannels, SlackTimestamp}

import scala.concurrent.Future

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
                     dataService: DataService
                   ) = {
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
    val event = SlackMessageEvent(botProfile, defaultChannel, None, defaultSlackUserId, "foo", SlackTimestamp.now)
    when(dataService.slackBotProfiles.allFor(team)).thenReturn(Future.successful(Seq(botProfile)))
    val loginInfo = LoginInfo(defaultContext, defaultSlackUserId)
    val slackProfile = SlackProfile(defaultSlackTeamId, loginInfo)
    when(dataService.slackProfiles.allFor(team.id)).thenReturn(Future.successful(Seq(slackProfile)))
    val linkedAccount = LinkedAccount(user, loginInfo, OffsetDateTime.now)
    when(dataService.linkedAccounts.maybeForSlackFor(user)).thenReturn(Future.successful(Some(linkedAccount)))
    when(dataService.slackProfiles.find(loginInfo)).thenReturn(Future.successful(Some(slackProfile)))
    val mockSlackChannels = mock[SlackChannels]
    when(dataService.slackBotProfiles.channelsFor(botProfile)).thenReturn(mockSlackChannels)
    val apiController = app.injector.instanceOf(classOf[APIController])
    implicit val actorSystem = apiController.actorSystem
    when(mockSlackChannels.maybeIdFor(defaultChannel)).thenReturn(Future.successful(Some(defaultChannel)))

    when(eventHandler.interruptOngoingConversationsFor(any[Event])).thenReturn(Future.successful(false))
    when(eventHandler.handle(any[Event], org.mockito.Matchers.eq(None))).thenReturn(Future.successful(Seq(SimpleTextResult(event, "result", forcePrivateResponse = false))))

    val mockSlackClient = mock[SlackApiClient]
    when(dataService.slackBotProfiles.clientFor(botProfile)).thenReturn(mockSlackClient)
    when(mockSlackClient.listIms).thenReturn(Future.successful(Seq()))
    when(mockSlackClient.postChatMessage(anyString, anyString, any[Option[String]], any[Option[Boolean]], any[Option[String]],
                                          any[Option[String]], any[Option[Seq[Attachment]]], any[Option[Boolean]], any[Option[Boolean]],
                                          any[Option[String]], any[Option[String]], any[Option[Boolean]], any[Option[Boolean]],
                                          any[Option[String]], any[Option[Boolean]])(any[ActorSystem])).thenReturn(Future.successful(SlackTimestamp.now))
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
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService)
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
        val token = setUpMocksFor(team, user, isTokenValid = true, None, app, eventHandler, dataService)
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
                        actionName: String,
                        channel: String,
                        token: String
                      ): JsValue = {
    JsObject(Seq(
      ("actionName", JsString(actionName)),
      ("responseContext", JsString(defaultContext)),
      ("channel", JsString(channel)),
      ("token", JsString(token))
    ))
  }

  "runAction" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService)
        val body = runActionBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.runAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, "group", None, None, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), None, None, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), None, None, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService)
        val actionName = "foo"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))

        val body = runActionBodyFor(actionName, defaultChannel, token)
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
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService)
        val body = postMessageBodyFor("foo", defaultChannel, token)
        val request = FakeRequest(controllers.routes.APIController.say()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = true, None, app, eventHandler, dataService)
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
                        actionName: String,
                        channel: String,
                        recurrenceString: String,
                        token: String
                      ): JsValue = {
    JsObject(Seq(
      ("actionName", JsString(actionName)),
      ("responseContext", JsString(defaultContext)),
      ("channel", JsString(channel)),
      ("recurrence", JsString(recurrenceString)),
      ("token", JsString(token))
    ))
  }

  "scheduleAction" should {

    "400 for invalid token" in new ControllerTestContext {
      running(app) {
        val token = setUpMocksFor(team, user, isTokenValid = false, None, app, eventHandler, dataService)
        val body = scheduleActionBodyFor("foo", defaultChannel, "every day at noon", token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Invalid token"
        verify(dataService.apiTokens, times(1)).find(token)
      }
    }

    "respond with a valid result" in new ControllerTestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, "group", None, None, None, team, OffsetDateTime.now)
        val originatingBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), None, None, OffsetDateTime.now)
        val targetBehavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), None, None, OffsetDateTime.now)
        val token = setUpMocksFor(team, user, isTokenValid = true, Some(originatingBehavior.id), app, eventHandler, dataService)
        val actionName = "foo"
        val recurrenceString = "every day at noon"
        when(dataService.behaviors.findWithoutAccessCheck(any[String])).thenReturn(Future.successful(None))
        when(dataService.behaviors.findWithoutAccessCheck(originatingBehavior.id)).thenReturn(Future.successful(Some(originatingBehavior)))
        when(dataService.behaviors.findByIdOrName(org.mockito.Matchers.eq(actionName), any[BehaviorGroup])).thenReturn(Future.successful(Some(targetBehavior)))
        when(dataService.users.ensureUserFor(any[LoginInfo], anyString)).thenReturn(Future.successful(user))
        val mockVersion = mock[BehaviorVersion]
        when(mockVersion.maybeName).thenReturn(Some(actionName))
        when(dataService.behaviors.maybeCurrentVersionFor(targetBehavior)).thenReturn(Future.successful(Some(mockVersion)))
        when(dataService.scheduledBehaviors.maybeCreateFor(targetBehavior, recurrenceString, user, team, Some(defaultChannel), false)).thenReturn {
          Future.successful(
            Some(
              ScheduledBehavior(
                IDs.next,
                targetBehavior,
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

        val body = scheduleActionBodyFor(actionName, defaultChannel, recurrenceString, token)
        val request = FakeRequest(controllers.routes.APIController.scheduleAction()).withJsonBody(body)
        val result = route(app, request).get
        status(result) mustBe OK

        verify(dataService.scheduledBehaviors, times(1)).maybeCreateFor(targetBehavior, recurrenceString, user, team, Some(defaultChannel), false)
      }
    }
  }

}
