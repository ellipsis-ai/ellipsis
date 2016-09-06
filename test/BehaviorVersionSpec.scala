import models.accounts.user.User
import models.bots.{Behavior, BehaviorQueries, BehaviorVersionQueries}
import models.IDs
import models.team.Team
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import services.DataService
import slick.driver.PostgresDriver.api.{Database => PostgresDatabase}


class BehaviorVersionSpec extends PlaySpec with DBMixin with OneAppPerSuite {

  val dataService = app.injector.instanceOf(classOf[DataService])

  def reloadBehavior(db: PostgresDatabase, behavior: Behavior): Behavior = {
    runNow(db, BehaviorQueries.findWithoutAccessCheck(behavior.id)).get
  }

  "BehaviorVersion" should {

    "should load the current version" in {
      withDatabase { db =>
        val team = runNow(dataService.teams.save(Team(IDs.next, "")))
        val user = runNow(dataService.users.save(User(IDs.next, team.id, None)))
        val behavior = runNow(db, BehaviorQueries.createFor(team, None))
        val firstVersion = runNow(db, BehaviorVersionQueries.createFor(behavior, Some(user)))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(db, BehaviorVersionQueries.createFor(behavior, Some(user)))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(secondVersion.id)
      }
    }

  }

}
