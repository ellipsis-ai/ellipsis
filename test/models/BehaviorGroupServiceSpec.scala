package models

import support.DBSpec

class BehaviorGroupServiceSpec extends DBSpec {

  "BehaviorGroupService.merge" should {

    "merge a list of groups into a new group" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam

        val groups = 1.to(3).map { _ => newSavedBehaviorGroupFor(team) }
        groups.foreach { group =>
          1.to(3).map { _ =>
            newSavedBehaviorFor(group)
            newSavedInputFor(group)
          }
        }
        groups.foreach { group =>
          runNow(dataService.behaviors.allForGroup(group)) must have length 3
          runNow(dataService.inputs.allForGroup(group)) must have length 3
        }

        val merged = runNow(dataService.behaviorGroups.merge(groups))

        groups.foreach { group =>
          runNow(dataService.behaviors.allForGroup(group)) mustBe empty
          runNow(dataService.inputs.allForGroup(group)) mustBe empty
          runNow(dataService.behaviorGroups.find(group.id)) mustBe empty
        }
        runNow(dataService.behaviors.allForGroup(merged)) must have length 9
        runNow(dataService.inputs.allForGroup(merged)) must have length 9
        runNow(dataService.behaviorGroups.find(merged.id)) must not be empty
      })
    }

  }

}
