package models

import support.DBSpec

class BehaviorGroupServiceSpec extends DBSpec {

  "BehaviorGroupService.merge" should {

    "merge a list of groups into a new group" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)

        val groups = 1.to(3).map { _ => newSavedBehaviorGroupFor(team) }
        val groupVersions = groups.map { ea => newSavedGroupVersionFor(ea, user) }

        groupVersions.foreach { ea =>
          1.to(3).map { _ =>
            val behavior = newSavedBehaviorFor(ea.group)
            val behaviorVersion = newSavedVersionFor(behavior, ea)
            newSavedParamFor(behaviorVersion)
          }
        }

        val reloadedGroupVersions = groupVersions.flatMap { ea =>
          runNow(dataService.behaviorGroupVersions.findWithoutAccessCheck(ea.id))
        }
        val reloadedGroups = reloadedGroupVersions.map(_.group)

        reloadedGroupVersions.foreach { ea =>
          runNow(dataService.behaviors.allForGroup(ea.group)) must have length 3
          runNow(dataService.inputs.allForGroupVersion(ea)) must have length 3
        }

        val merged = runNow(dataService.behaviorGroups.merge(reloadedGroups, user))
        val mergedVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(merged)).get

        reloadedGroupVersions.foreach { groupVersion =>
          runNow(dataService.behaviors.allForGroup(groupVersion.group)) mustBe empty
          runNow(dataService.inputs.allForGroupVersion(groupVersion)) mustBe empty
          runNow(dataService.behaviorGroups.find(groupVersion.group.id)) mustBe empty
        }
        runNow(dataService.behaviors.allForGroup(merged)) must have length 9
        runNow(dataService.inputs.allForGroupVersion(mergedVersion)) must have length 9
        runNow(dataService.behaviorGroups.find(merged.id)) must not be empty
      })
    }

  }

}
