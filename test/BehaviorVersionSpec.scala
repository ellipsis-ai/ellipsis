import models.bots.triggers.MessageTriggerQueries
import models.bots.{BehaviorParameterQueries, Behavior, BehaviorVersionQueries, BehaviorQueries}
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
        val firstTrigger = runNow(db, MessageTriggerQueries.createFor(firstVersion, "first", false, false, false))
        runNow(db, MessageTriggerQueries.allFor(firstVersion)).map(_.pattern) mustBe Seq("first")
        val firstParam = runNow(db, BehaviorParameterQueries.createFor("first", None, 1, firstVersion))
        runNow(db, BehaviorParameterQueries.allFor(firstVersion)).map(_.name) mustBe Seq("first")

        var secondVersion = runNow(db, BehaviorVersionQueries.createFor(behavior))
        secondVersion = runNow(db, secondVersion.copy(maybeFunctionBody = secondCode).save)
        runNow(db, reloadBehavior(db, behavior).maybeCurrentVersion).get.maybeFunctionBody mustBe secondCode
        val secondTrigger = runNow(db, MessageTriggerQueries.createFor(secondVersion, "second", false, false, false))
        runNow(db, MessageTriggerQueries.allFor(secondVersion)).map(_.pattern) mustBe Seq("second")
        val secondParam = runNow(db, BehaviorParameterQueries.createFor("second", None, 1, secondVersion))
        runNow(db, BehaviorParameterQueries.allFor(secondVersion)).map(_.name) mustBe Seq("second")

        runNow(db, firstVersion.restore)
        val restoredVersion = runNow(db, reloadBehavior(db, behavior).maybeCurrentVersion).get

        // code is restored
        restoredVersion.maybeFunctionBody mustBe firstCode
        // triggers restored too
        runNow(db, MessageTriggerQueries.allFor(restoredVersion)).map(_.pattern) mustBe Seq("first")
        // and params
        runNow(db, BehaviorParameterQueries.allFor(restoredVersion)).map(_.name) mustBe Seq("first")
        // new ID for new version instance
        restoredVersion.id mustNot be(Some(firstVersion.id))
      }
    }

  }

}
