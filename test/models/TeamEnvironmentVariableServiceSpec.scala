package models

import json.BehaviorVersionData
import support.DBSpec

class TeamEnvironmentVariableServiceSpec extends DBSpec {

  "TeamEnvironmentVariableService.missingInAction" should {
    "return the set of missing team environment variables" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, None, dataService).copy(
          functionBody =
            """
              |ellipsis.success(
              |  ellipsis.env.KNOWN_THING +
              |  ellipsis.env.SOME_UNKNOWN_THING +
              |  ellipsis.env.SOME_UNKNOWN_THING
              |);
              |""".stripMargin
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )))
        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        runNow(dataService.teamEnvironmentVariables.ensureFor("KNOWN_THING", Some("foo"), team))
        runNow(dataService.teamEnvironmentVariables.missingInAction(behaviorVersion, dataService)) mustBe Set("SOME_UNKNOWN_THING")
      })
    }
  }

}
