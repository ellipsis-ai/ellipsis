package models

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.accounts.{SlackProfile, SlackProfileQueries, SlackProvider}
import models.accounts.linkedaccount.LinkedAccount
import models.team.Team
import modules.ActorModule
import org.joda.time.DateTime
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import services.PostgresDataService
import support.DBMixin

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LinkedAccountServiceSpec extends PlaySpec with DBMixin with OneAppPerSuite {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder().
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])

  // TODO: use mocks once data service work is done

  def newSavedTeam: Future[Team] = dataService.teams.create(IDs.next)

  def newSavedUserFor(teamId: String): Future[User] = {
    dataService.users.createFor(teamId)
  }

  def newSavedLinkedAccountFor(user: User): Future[LinkedAccount] = {
    val account = LinkedAccount(user, LoginInfo(SlackProvider.ID, IDs.next), DateTime.now)
    dataService.linkedAccounts.save(account)
  }

  def newSavedLinkedAccount: LinkedAccount = {
    runNow(for {
      team <- newSavedTeam
      user <- newSavedUserFor(team.id)
      linkedAccount <- newSavedLinkedAccountFor(user)
    } yield linkedAccount)
  }

  "LinkedAccountServiceSpec.isAdmin" should {

    "be false when there's no SlackProfile " in {
      withEmptyDB(dataService, { db =>
        val linkedAccount = newSavedLinkedAccount
        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
      })
    }

    "be false when the SlackProfile is for the wrong Slack team" in {
      withEmptyDB(dataService, { db =>
        val linkedAccount = newSavedLinkedAccount
        val randomSlackTeamId = IDs.next
        runNow(db, SlackProfileQueries.save(SlackProfile(randomSlackTeamId, linkedAccount.loginInfo)))

        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe false
      })
    }

    "be true when there's a matching SlackProfile for the admin Slack team" in {
      withEmptyDB(dataService, { db =>
        val linkedAccount = newSavedLinkedAccount
        runNow(db, SlackProfileQueries.save(SlackProfile(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, linkedAccount.loginInfo)))

        runNow(dataService.linkedAccounts.isAdmin(linkedAccount)) mustBe true
      })
    }

  }

}
