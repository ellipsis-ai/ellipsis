package models.billing.active_user_records

import java.time.OffsetDateTime

import models.IDs
import models.accounts.user.User
import models.billing.active_user_record.ActiveUserRecord
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

  "ActiveUserRecordService.countFor" should {

    "return 0 " in {
      withEmptyDB(dataService, { () =>
        val org = runNow(dataService.organizations.create(name = "myOrg", chargebeeCustomerId = IDs.next))
        val team = runNow(dataService.teams.create("myTeam", org))
        val users: Seq[User] = newUsersFor(team, 10)
        users.length mustBe 10

        simulateActivityFor(users.take(5))

        runNow(dataService.activeUserRecords.allRecords).length mustBe 5

        runNow(dataService.activeUserRecords.countFor(team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 5

        simulateActivityFor(Seq(users.head, users.head, users.head))
        simulateActivityFor(users.take(5))

        runNow(dataService.activeUserRecords.countFor(team.id, OffsetDateTime.now.minusMonths(1), OffsetDateTime.now)) mustBe 5

      })
    }
  }
}
