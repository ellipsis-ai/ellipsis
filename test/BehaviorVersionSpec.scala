import models.bots.{Behavior, BehaviorVersionQueries, BehaviorQueries}
import models.{Team, IDs}
import org.scalatestplus.play.PlaySpec
import slick.driver.PostgresDriver.api.{Database => PostgresDatabase}


class BehaviorVersionSpec extends PlaySpec with DBMixin {

  def reloadBehavior(db: PostgresDatabase, behavior: Behavior): Behavior = {
    runNow(db, BehaviorQueries.findWithoutAccessCheck(behavior.id)).get
  }

  "BehaviorVersion" should {

    "should load the current version" in {
      withDatabase { db =>
        val team = runNow(db, Team(IDs.next, "").save)
        val behavior = runNow(db, BehaviorQueries.createFor(team))
        val firstVersion = runNow(db, BehaviorVersionQueries.createFor(behavior))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(db, BehaviorVersionQueries.createFor(behavior))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(secondVersion.id)
      }
    }

    "should restore to a particular version" in {
      withDatabase { db =>
        val firstCode = Some("first code")
        val secondCode = Some("second code")
        val team = runNow(db, Team(IDs.next, "").save)
        val behavior = runNow(db, BehaviorQueries.createFor(team))

        var firstVersion = runNow(db, BehaviorVersionQueries.createFor(behavior))
        firstVersion = runNow(db, firstVersion.copy(maybeFunctionBody = firstCode).save)
        runNow(db, reloadBehavior(db, behavior).maybeCurrentVersion).get.maybeFunctionBody mustBe firstCode

        var secondVersion = runNow(db, BehaviorVersionQueries.createFor(behavior))
        secondVersion = runNow(db, secondVersion.copy(maybeFunctionBody = secondCode).save)
        runNow(db, reloadBehavior(db, behavior).maybeCurrentVersion).get.maybeFunctionBody mustBe secondCode

        runNow(db, firstVersion.restore)
        // code is restored
        runNow(db, reloadBehavior(db, behavior).maybeCurrentVersion).get.maybeFunctionBody mustBe firstCode
        // new ID for new version instance
        reloadBehavior(db, behavior).maybeCurrentVersionId mustNot be(Some(firstVersion.id))
      }
    }

  }

}
