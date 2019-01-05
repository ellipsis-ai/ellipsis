import java.time._

import akka.actor.ActorSystem
import json.UserData
import models.IDs
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.ellipsisobject._
import models.behaviors.events.{EventType, TestEventContext}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.testing.TestMessageEvent
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.Logger
import play.api.libs.json.Json
import services.DefaultServices
import slick.dbio.DBIO
import support.{DBSpec, TestContext}

import scala.concurrent.{ExecutionContext, Future}

class EllipsisObjectSpec extends DBSpec {

  val messageText: String = "foo bar"
  val platformName: String = "test"
  val platformDesc: String = "Test"
  val maybeChannel: Option[String] = Some("testchannel")
  val maybeThread: Option[String] = None
  val userIdForContext: String = "UTEST"
  val maybePermalink: Option[String] = Some("perma.link")
  val maybeReactionAdded: Option[String] = None
  val maybeConversation: Option[Conversation] = None

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
    when(services.dataService.linkedOAuth1Tokens.allForUserAction(user, services.ws)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedOAuth2Tokens.allForUserAction(user, services.ws)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedSimpleTokens.allForUserAction(user)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedAccounts.allForAction(user)).thenReturn(DBIO.successful(Seq()))
  }

  "toJson" should {

    "build a valid object" in new TestContext {
      val event = mock[TestMessageEvent]
      setUpMocksFor(event, user, team, services)
      runNow(for {
        userInfo <- DeprecatedUserInfo.buildForAction(event, maybeConversation, services)
        teamInfo <- DBIO.successful(TeamInfo(team, Seq(), Seq(), None))
        eventUser <- EventUser.buildForAction(event, maybeConversation, services)
      } yield {
        val eventInfo = EventInfo(event, eventUser, None)
        val token = InvocationToken(IDs.next, user.id, IDs.next, None, None, OffsetDateTime.now)
        val json = EllipsisObject(userInfo, teamInfo, eventInfo, Seq(), "test.ellipsis", token).toJson
        Logger.info(Json.prettyPrint(json))
        (json \ "event" \ "user" \ "ellipsisUserId").as[String] mustBe user.id
        (json \ "userInfo" \ "ellipsisUserId").as[String] mustBe user.id
      })
    }
  }

}
