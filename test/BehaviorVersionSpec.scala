import javax.inject.Inject

import models.accounts.user.User
import models.bots.{Behavior, BehaviorQueries, BehaviorVersionQueries}
import models.{IDs, Team}
import org.scalatestplus.play.PlaySpec
import services.DataService
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api.{Database => PostgresDatabase}


class BehaviorVersionSpec @Inject() (dataService: DataService) extends PlaySpec with DBMixin {

  def reloadBehavior(db: PostgresDatabase, behavior: Behavior): Behavior = {
    runNow(db, BehaviorQueries.findWithoutAccessCheck(behavior.id)).get
  }

  "BehaviorVersion" should {

    "should load the current version" in {
      withDatabase { db =>
        val team = runNow(db, Team(IDs.next, "").save)
        val user = runNow(db, DBIO.from(dataService.users.save(User(IDs.next, team.id, None))))
        val behavior = runNow(db, BehaviorQueries.createFor(team, None))
        val firstVersion = runNow(db, BehaviorVersionQueries.createFor(behavior, Some(user)))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(db, BehaviorVersionQueries.createFor(behavior, Some(user)))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(secondVersion.id)
      }
    }

  }

}
