import java.time._

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.UserData
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.ellipsisobject._
import models.behaviors.events.{EventType, TestEventContext}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.recurrence.Minutely
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.testing.TestMessageEvent
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.Logger
import play.api.libs.json.Json
import services.DefaultServices
import slick.dbio.DBIO
import support.{BehaviorGroupDataBuilder, DBSpec, TestContext}
import utils.SlackTimestamp

import scala.concurrent.{ExecutionContext, Future}

class EllipsisObjectSpec extends DBSpec {

  val messageText: String = "foo bar"
  val platformName: String = "test"
  val platformDesc: String = "Test"
  val channel: String = "testchannel"
  val maybeChannel: Option[String] = Some(channel)
  val maybeMessageId: Option[String] = Some(SlackTimestamp.now)
  val maybeThread: Option[String] = None
  val userIdForContext: String = "UTEST"
  val maybePermalink: Option[String] = Some("perma.link")
  val maybeReactionAdded: Option[String] = None
  val maybeConversation: Option[Conversation] = None
  val recurrence = Minutely(IDs.next, 1, 0, Some(1))

  def setUpMocksFor(
                   event: TestMessageEvent,
                   user: User,
                   team: Team,
                   services: DefaultServices
                   ): Unit = {
    when(event.ensureUserAction(services.dataService)).thenReturn(DBIO.successful(user))
    when(services.dataService.teams.findAction(user.teamId)).thenReturn(DBIO.successful(Some(team)))
    when(services.dataService.users.userDataFor(user, team)).thenReturn(Future.successful(UserData.withoutProfile(user.id)))
    when(event.deprecatedMessageInfoAction(org.mockito.Matchers.eq(maybeConversation), any[DefaultServices])(any[ActorSystem], any[ExecutionContext])).thenReturn(DBIO.successful(DeprecatedMessageInfo(
      messageText,
      platformName,
      platformDesc,
      maybeChannel,
      maybeThread,
      userIdForContext,
      Json.obj(),
      Set(),
      maybePermalink,
      maybeReactionAdded
    )))
    when(event.originalEventType).thenReturn(EventType.test)
    val eventContext = mock[TestEventContext]
    when(event.eventContext).thenReturn(eventContext)
    when(eventContext.name).thenReturn(platformName)
    when(eventContext.description).thenReturn(platformDesc)
    when(eventContext.team).thenReturn(team)
    when(event.maybeScheduled).thenReturn(Some(ScheduledMessage(
      IDs.next,
      messageText,
      Some(user),
      team,
      maybeChannel,
      isForIndividualMembers = false,
      recurrence,
      OffsetDateTime.now,
      OffsetDateTime.now
    )))
    when(services.dataService.linkedOAuth1Tokens.allForUserAction(user, services.ws)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedOAuth2Tokens.allForUserAction(user, services.ws)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedSimpleTokens.allForUserAction(user)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedAccounts.allForAction(user)).thenReturn(DBIO.successful(Seq(
      LinkedAccount(user, LoginInfo(platformName, userIdForContext), OffsetDateTime.now)
    )))
  }

  "buildFor" should {

    "build a valid object" in new TestContext {
      val event = mock[TestMessageEvent]
      setUpMocksFor(event, user, team, services)
      runNow(for {
        userInfo <- DeprecatedUserInfo.buildForAction(event, maybeConversation, services)
        teamInfo <- DBIO.successful(TeamInfo(team.id, Seq(), Map(), None, None, Some(team.timeZone.toString)))
        eventUser <- EventUser.buildForAction(event, maybeConversation, services)
      } yield {
        val maybeChannelObj = Some(Channel(channel, maybeChannel, Some(s"<@$channel>"), None))
        val maybeMessage = Some(MessageObject(messageText, maybeMessageId, maybeChannelObj, maybeThread, usersMentioned = Set(), permalink = maybePermalink, reactionAdded = None))
        val eventInfo = EventInfo.buildFor(event, eventUser, maybeMessage, configuration)
        val token = InvocationToken(IDs.next, user.id, IDs.next, None, None, OffsetDateTime.now)
        val behaviorGroupData = BehaviorGroupDataBuilder.buildFor(team.id)
        val actionId = behaviorGroupData.actionBehaviorVersions.head.behaviorId.get
        val actionInfo = CurrentActionInfo(actionId, SkillInfo.fromBehaviorGroupData(behaviorGroupData))
        val json = Json.toJson(EllipsisObject.buildFor(userInfo, teamInfo, eventInfo, actionInfo, Seq(), "test.ellipsis", token))
        Logger.info(Json.prettyPrint(json))

        val resultObject = json.as[EllipsisObject]

        val resultEvent = resultObject.event
        resultEvent.originalEventType mustBe EventType.test.toString
        resultEvent.platformName mustBe platformName
        resultEvent.platformDescription mustBe platformDesc

        val resultEventUser = resultEvent.user
        resultEventUser.ellipsisUserId mustBe user.id
        resultEventUser.identities must have length(1)
        val resultIdentity = resultEventUser.identities.head
        resultIdentity.platform mustBe platformName
        resultIdentity.id must contain(userIdForContext)

        val resultEventMessage = resultEvent.message.get
        resultEventMessage.text mustBe messageText
        resultEventMessage.channel.flatMap(_.name) must contain(channel)
        resultEventMessage.permalink mustBe maybePermalink

        val scheduledEditUrl = controllers.routes.ScheduledActionsController.index(
          selectedId = event.maybeScheduled.map(_.id),
          newSchedule = None,
          channelId = maybeChannel,
          teamId = Some(event.ellipsisTeamId),
          forceAdmin = None
        ).url
        val resultEventSchedule = resultEvent.schedule.get
        resultEventSchedule.editLink must endWith(scheduledEditUrl)
        resultEventSchedule.recurrence mustEqual recurrence.displayString

        val resultActionInfo = resultObject.action
        resultActionInfo.actionId mustBe actionId
        resultActionInfo.skill mustBe behaviorGroupData

        // deprecated stuff:
        val resultUserInfo = resultObject.userInfo
        resultUserInfo.ellipsisUserId mustBe user.id
        resultUserInfo.links must have length(1)
        val link = resultUserInfo.links.head
        link.platform mustBe platformName
        link.id must contain(userIdForContext)
        val resultMessageInfo = resultUserInfo.messageInfo.get
        resultMessageInfo.text mustBe messageText
        resultMessageInfo.medium mustBe platformName
        resultMessageInfo.mediumDescription mustBe platformDesc
        resultMessageInfo.channel must contain(channel)
        resultMessageInfo.userId mustBe userIdForContext
        resultMessageInfo.permalink mustBe maybePermalink

        val resultTeam = resultObject.team
        resultTeam.timeZone must contain(team.timeZone.toString)
        resultTeam.links must have length(0)

      })
    }
  }

}
