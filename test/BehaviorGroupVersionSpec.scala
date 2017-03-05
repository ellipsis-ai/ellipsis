import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import models.behaviors.behaviorgroup.BehaviorGroup
import support.DBSpec

class BehaviorGroupVersionSpec extends DBSpec {

  def reloadGroup(db: PostgresDatabase, group: BehaviorGroup): BehaviorGroup = {
    runNow(dataService.behaviorGroups.find(group.id)).get
  }

  "BehaviorGroupVersion" should {

    "should load the current version" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val firstVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        reloadGroup(db, group).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        reloadGroup(db, group).maybeCurrentVersionId mustBe Some(secondVersion.id)
      })
    }

  }

}
