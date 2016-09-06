import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.IDs
import models.accounts.{SlackProfile, SlackProfileQueries, SlackProvider}
import models.accounts.linkedaccount.LinkedAccount
import models.team.Team
import org.joda.time.DateTime
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import services.DataService

class LinkedAccountServiceSpec extends PlaySpec with DBMixin with OneAppPerSuite {

  val dataService = app.injector.instanceOf(classOf[DataService])

  // TODO: use mocks once data service work is done

  def newSavedTeam: Team = runNow(dataService.teams.create(IDs.next))

  def newSavedUserFor(teamId: String): User = {
    runNow(dataService.users.createFor(teamId))
  }

  def newSavedLinkedAccountFor(user: User): LinkedAccount = {
    val account = LinkedAccount(user, LoginInfo(SlackProvider.ID, IDs.next), DateTime.now)
    runNow(dataService.linkedAccounts.save(account))
  }

  def newSavedLinkedAccount: LinkedAccount = {
    val team = newSavedTeam
    val user = newSavedUserFor(team.id)
    newSavedLinkedAccountFor(user)
  }

  "LinkedAccountServiceSpec.isAdmin" should {

    "be false when there's no SlackProfile " in {
      withDatabase { db =>
        val linkedAccount = newSavedLinkedAccount
        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
      }
    }

    "be false when the SlackProfile is for the wrong Slack team" in {
      withDatabase { db =>
        val linkedAccount = newSavedLinkedAccount
        val randomSlackTeamId = IDs.next
        runNow(db, SlackProfileQueries.save(SlackProfile(randomSlackTeamId, linkedAccount.loginInfo)))

        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
      }
    }

    "be true when there's a matching SlackProfile for the admin Slack team" in {
      withDatabase { db =>
        val linkedAccount = newSavedLinkedAccount
        runNow(db, SlackProfileQueries.save(SlackProfile(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, linkedAccount.loginInfo)))

        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe true
      }
    }

  }

}
