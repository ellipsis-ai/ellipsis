import models.behaviors.behavior.Behavior
import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import support.DBSpec

class BehaviorVersionSpec extends DBSpec {

  def reloadBehavior(db: PostgresDatabase, behavior: Behavior): Behavior = {
    runNow(dataService.behaviors.findWithoutAccessCheck(behavior.id)).get
  }

  "BehaviorVersion" should {

    "should load the current version" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val firstVersion = runNow(dataService.behaviorVersions.createFor(behavior, Some(user)))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(dataService.behaviorVersions.createFor(behavior, Some(user)))
        reloadBehavior(db, behavior).maybeCurrentVersionId mustBe Some(secondVersion.id)
      })
    }

  }

}
