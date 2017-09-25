package models

import json.BehaviorVersionData
import support.DBSpec

class UserEnvironmentVariableServiceSpec extends DBSpec {

  "UserEnvironmentVariableService.missingInAction" should {
    "return the set of missing user environment variables" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, None, dataService).copy(
          functionBody =
            """
              |ellipsis.success(
              |  ellipsis.userEnv.KNOWN_THING +
              |  ellipsis.userEnv.SOME_UNKNOWN_THING +
              |  ellipsis.userEnv.SOME_UNKNOWN_THING
              |);
              |""".stripMargin
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )))
        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        runNow(dataService.userEnvironmentVariables.ensureFor("KNOWN_THING", Some("foo"), user))
        runNow(dataService.userEnvironmentVariables.missingForAction(user, behaviorVersion, dataService)) mustBe Set("SOME_UNKNOWN_THING")
      })
    }
  }

}
