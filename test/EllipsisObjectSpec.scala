import java.time._

import models.IDs
import models.accounts.user.User
import models.behaviors.ellipsisobject._
import models.behaviors.events.Event
import models.behaviors.invocationtoken.InvocationToken
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.DefaultServices
import slick.dbio.DBIO
import support.TestContext

class EllipsisObjectSpec extends PlaySpec with MockitoSugar {

  def setUpMocksFor(
                   event: Event,
                   user: User,
                   services: DefaultServices
                   ): Unit = {
    when(event.ensureUserAction(services.dataService)).thenReturn(DBIO.successful(user))
    when(services.dataService.linkedOAuth1Tokens.allForUserAction(user, services.ws)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedOAuth2Tokens.allForUserAction(user, services.ws)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedSimpleTokens.allForUserAction(user)).thenReturn(DBIO.successful(Seq()))
    when(services.dataService.linkedAccounts.allForAction(user)).thenReturn(DBIO.successful(Seq()))
  }

  "toJson" should {

    "build a valid object" in new TestContext {
      val event = mock[Event]
      setUpMocksFor(event, user, services)
      for {
        userInfo <- DeprecatedUserInfo.buildForAction(event, None, services)
        teamInfo <- DBIO.successful(TeamInfo(team, Seq(), Seq(), None))
        eventUser <- EventUser.buildForAction(event, None, services)
      } yield {
        val eventInfo = EventInfo(event, eventUser, None)
        val token = InvocationToken(IDs.next, user.id, IDs.next, None, None, OffsetDateTime.now)
        val json = EllipsisObject(userInfo, teamInfo, eventInfo, Seq(), "test.ellipsis", token).toJson
        (json \ "event" \ "user" \ "ellipsisUserId").as[String] mustBe user.id
        (json \ "userInfo" \ "ellipsisUserId").as[String] mustBe user.id
      }
    }
  }

}
