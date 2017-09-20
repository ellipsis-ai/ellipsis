package models

import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import json.{BehaviorGroupData, BehaviorParameterTypeData, BehaviorVersionData, DataTypeFieldData}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.TextType
import play.api.libs.json.Json
import support.DBSpec

class BehaviorGroupVersionSpec extends DBSpec {

  def reloadGroup(group: BehaviorGroup): BehaviorGroup = {
    runNow(dataService.behaviorGroups.findWithoutAccessCheck(group.id)).get
  }

  "createFor" should {

    "set the current version on the group" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val firstVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        reloadGroup(group).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        reloadGroup(group).maybeCurrentVersionId mustBe Some(secondVersion.id)
      })
    }

    "maintain saved answers" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor()
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, maybeName = None, dataService).copy(
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
        val secondGroupVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user, groupVersionData, forceNodeModuleUpdate = false))
        val maybeSecondInputVersion = runNow(dataService.inputs.allForGroupVersion(secondGroupVersion)).headOption

        maybeSecondInputVersion.isDefined mustBe true
        val maybeSecondSavedAnswerVersion = runNow(dataService.savedAnswers.find(maybeSecondInputVersion.get, user))
        maybeSecondSavedAnswerVersion.map(_.valueString) mustBe Some(savedAnswer.valueString)
      })
    }

    "keep data type behavior version id in sync with latest group version" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val dataTypeVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = true, maybeName = Some("A data type"), dataService)

        val dataTypeParamData = BehaviorParameterTypeData(
          dataTypeVersionData.id,
          dataTypeVersionData.exportId,
          dataTypeVersionData.name.get,
          None
        )

        val inputData = newInputDataFor(Some(dataTypeParamData))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, maybeName = None, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(dataTypeVersionData, behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        newSavedGroupVersionFor(group, user, Some(groupData))

        val groupVersionsBefore = runNow(dataService.behaviorGroupVersions.allFor(group))
        groupVersionsBefore must have length 1
        val firstDataTypeBehaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersionsBefore.head)).filter(_.isDataType).head

        val newGroupData = runNow(BehaviorGroupData.maybeFor(group.id, user, None, dataService))

        newSavedGroupVersionFor(group, user, newGroupData)

        val groupVersionsAfter = runNow(dataService.behaviorGroupVersions.allFor(group))
        groupVersionsAfter must have length 2

        val secondGroupVersion = groupVersionsAfter.sortBy(_.createdAt).reverse.head
        val secondDataTypeBehaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(secondGroupVersion)).filter(_.isDataType).head
        secondDataTypeBehaviorVersion.id mustNot be(firstDataTypeBehaviorVersion.id)

        val secondInput = runNow(dataService.inputs.allForGroupVersion(secondGroupVersion)).head
        secondInput.paramType.id mustBe secondDataTypeBehaviorVersion.id
      })
    }

    "ensures that data type names have the same format" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val dataTypeVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = true, maybeName = Some("_a data type"), dataService)

        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(dataTypeVersionData)
        )
        val saved = newSavedGroupVersionFor(group, user, Some(groupData))

        runNow(dataService.behaviorVersions.dataTypesForGroupVersionAction(saved)).head.maybeName mustBe Some("Adatatype")
      })
    }

    "maintains default storage data" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val dataTypeVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = true, maybeName = Some("A data type"), dataService)
        val defaultStorageDataTypeVersionData = dataTypeVersionData.copy(
          config = dataTypeVersionData.config.copy(
            dataTypeConfig = dataTypeVersionData.config.dataTypeConfig.map { cfg =>
              cfg.copy(
                usesCode = Some(false),
                fields = Seq(
                  DataTypeFieldData.newUnsavedNamed("name", runNow(BehaviorParameterTypeData.from(TextType, dataService)))
                )
              )
            }
          )
        )

        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(defaultStorageDataTypeVersionData)
        )

        val firstGroupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val firstBehaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(firstGroupVersion)).head
        val firstDataTypeConfig = runNow(dataService.dataTypeConfigs.maybeFor(firstBehaviorVersion)).get
        val firstDataTypeFields = runNow(dataService.dataTypeFields.allFor(firstDataTypeConfig))

        val behavior = runNow(dataService.behaviors.allForGroup(firstGroupVersion.group)).head
        val savedItem = runNow(dataService.defaultStorageItems.createItemForBehavior(behavior, user, Json.toJson(Map("name" -> "foo"))))

        val groupVersionData = runNow(BehaviorGroupData.buildFor(firstGroupVersion, user, dataService)).copyForNewVersionOf(group)
        val secondGroupVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user, groupVersionData, forceNodeModuleUpdate = false))

        val secondBehaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(secondGroupVersion)).head
        val secondDataTypeConfig = runNow(dataService.dataTypeConfigs.maybeFor(secondBehaviorVersion)).get
        val secondDataTypeFields = runNow(dataService.dataTypeFields.allFor(secondDataTypeConfig))

        firstDataTypeFields.map(_.fieldId).toSet mustBe secondDataTypeFields.map(_.fieldId).toSet

        val itemForSecondVersion = runNow(dataService.defaultStorageItems.allFor(behavior)).head

        savedItem.data mustBe itemForSecondVersion.data
      })
    }

  }

}
