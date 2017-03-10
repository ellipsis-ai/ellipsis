package models

import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import json.BehaviorGroupData
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
        val behavior = newSavedBehaviorFor(group)
        val firstGroupVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        val firstBehaviorVersion = newSavedVersionFor(behavior, firstGroupVersion)
        val input = newSavedInputFor(firstGroupVersion)
        val param = newSavedParamFor(firstBehaviorVersion, maybeExistingInput = Some(input))
        val savedAnswer = newSavedAnswerFor(input, user)
        runNow(dataService.savedAnswers.find(input, user)).map(_.valueString) mustBe Some(savedAnswer.valueString)

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
