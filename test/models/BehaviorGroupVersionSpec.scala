package models

import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import json.{BehaviorGroupData, BehaviorVersionData}
import models.behaviors.behaviorgroup.BehaviorGroup
import support.DBSpec

class BehaviorGroupVersionSpec extends DBSpec {

  def reloadGroup(db: PostgresDatabase, group: BehaviorGroup): BehaviorGroup = {
    runNow(dataService.behaviorGroups.find(group.id)).get
  }

  "createFor" should {

    "should set the current version on the group" in {
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

    "should maintain saved answers" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor()
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val firstGroupVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val maybeInput = runNow(dataService.inputs.allForGroupVersion(firstGroupVersion)).headOption
        maybeInput.isDefined mustBe true

        val savedAnswer = newSavedAnswerFor(maybeInput.get, user)
        runNow(dataService.savedAnswers.find(maybeInput.get, user)).map(_.valueString) mustBe Some(savedAnswer.valueString)

        val groupVersionData = runNow(BehaviorGroupData.buildFor(firstGroupVersion, user, dataService)).copyForNewVersionOf(group)
        val secondGroupVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user, groupVersionData))
        val maybeSecondInputVersion = runNow(dataService.inputs.allForGroupVersion(secondGroupVersion)).headOption

        maybeSecondInputVersion.isDefined mustBe true
        val maybeSecondSavedAnswerVersion = runNow(dataService.savedAnswers.find(maybeSecondInputVersion.get, user))
        maybeSecondSavedAnswerVersion.map(_.valueString) mustBe Some(savedAnswer.valueString)
      })
    }

  }

}
