package models.billing.active_user_records

import java.time.OffsetDateTime

import models.IDs
import models.accounts.user.User
import models.billing.active_user_record.ActiveUserRecord
import models.organization.Organization
import models.team.Team
import support.DBSpec

import scala.concurrent.Future


class ActiveUserRecordServiceSpec extends DBSpec {

  def newUsersFor(team: Team, count: Int): Seq[User] = {
    runNow(Future.sequence((1 to count).map(_ => dataService.users.createFor(team.id))))
  }

  def simulateActivityFor(users: Seq[User]): Seq[ActiveUserRecord] = {
    runNow(Future.sequence(users.map(dataService.activeUserRecords.create(_, OffsetDateTime.now))))
  }

  case class FatTeam(org: Organization, team: Team, users: Seq[User])

  def setupTeams(count: Int, usersPerTeam: Int): Seq[FatTeam] = {
    runNow(
      Future.sequence {
      (1 to count).map { i=>
        for {
          org <- dataService.organizations.create(name = s"myOrg-${i}", chargebeeCustomerId = IDs.next)
          team <- dataService.teams.create(s"myTeam-${i}", org)
          users <- Future.sequence((1 to usersPerTeam).map(_ => dataService.users.createFor(team.id)))
        } yield FatTeam(org, team, users)
      }
    })
  }

  "ActiveUserRecordService.countFor" should {

    "counts the number of active user records for a team" in {
      withEmptyDB(dataService, { () =>
        val fatTeams = setupTeams(2, 10)
        val fatTeam1 = fatTeams(0)
        val fatTeam2 = fatTeams(1)

        fatTeam1.users.length mustBe 10
        fatTeam2.users.length mustBe 10
        simulateActivityFor(fatTeam1.users.take(5))
        runNow(dataService.activeUserRecords.allRecords).length mustBe 5
        runNow(dataService.activeUserRecords.countFor(fatTeam1.team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 5
        runNow(dataService.activeUserRecords.countFor(fatTeam2.team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 0

        simulateActivityFor(fatTeam2.users.take(3))

        runNow(dataService.activeUserRecords.countFor(fatTeam1.team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 5
        runNow(dataService.activeUserRecords.countFor(fatTeam2.team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 3

      })
    }

    "counts only unique users" in {
      withEmptyDB(dataService, { () =>
        val fatTeams = setupTeams(1, 10)
        val fatTeam1 = fatTeams(0)

        simulateActivityFor(fatTeam1.users.take(5))
        runNow(dataService.activeUserRecords.countFor(fatTeam1.team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 5

        simulateActivityFor(fatTeam1.users.take(5))
        simulateActivityFor(fatTeam1.users.take(5))

        runNow(dataService.activeUserRecords.countFor(fatTeam1.team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 5
      })
    }
  }
}
