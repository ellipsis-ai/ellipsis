import export.{BehaviorGroupExporter, BehaviorGroupZipImporter}
import json.BehaviorParameterTypeData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType}
import support.DBSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorGroupImportExportSpec extends DBSpec {

  def exportAndImport(group: BehaviorGroup, exportUser: User, importUser: User): Unit = {
    val maybeImportTeam = runNow(dataService.teams.find(importUser.teamId))
    val maybeExporter = runNow(BehaviorGroupExporter.maybeFor(group.id, exportUser, dataService))
    maybeExporter.isDefined mustBe(true)
    val file = maybeExporter.get.getZipFile
    val importer = BehaviorGroupZipImporter(maybeImportTeam.get, importUser, file, dataService)
    runNow(importer.run)
  }

  def checkImportedBehaviorsCorrectly(exported: BehaviorGroup, imported: BehaviorGroup): Unit = {
    val exportedBehaviors = runNow(dataService.behaviors.regularForGroup(exported))
    val importedBehaviors = runNow(dataService.behaviors.regularForGroup(imported))
    exportedBehaviors.length mustBe importedBehaviors.length
    exportedBehaviors.map(_.id).intersect(importedBehaviors.map(_.id)) mustBe empty
  }

  def checkImportedDataTypesCorrectly(exported: BehaviorGroup, imported: BehaviorGroup): Unit = {
    val exportedDataTypes = runNow(dataService.behaviors.dataTypesForGroup(exported))
    val importedDataTypes = runNow(dataService.behaviors.dataTypesForGroup(imported))
    exportedDataTypes.length mustBe importedDataTypes.length
    exportedDataTypes.map(_.id).intersect(importedDataTypes.map(_.id)) mustBe empty
  }

  def checkImportedInputsCorrectly(exported: BehaviorGroup, imported: BehaviorGroup): Unit = {
    val exportedInputs = runNow(dataService.inputs.allForGroup(exported))
    val importedInputs = runNow(dataService.inputs.allForGroup(imported))
    exportedInputs.length mustBe importedInputs.length
    exportedInputs.map(_.id).intersect(importedInputs.map(_.id)) mustBe empty
  }

  def mustBeValidImport(exported: BehaviorGroup, imported: BehaviorGroup): Unit = {
    exported.id must not be(imported.id)
    imported.maybeImportedId must contain(exported.id)

    checkImportedBehaviorsCorrectly(exported, imported)
    checkImportedDataTypesCorrectly(exported, imported)
    checkImportedInputsCorrectly(exported, imported)
  }

  "BehaviorGroupExporter" should {

    "export and import back in" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior1 = newSavedBehaviorFor(group)
        val version1 = newSavedVersionFor(behavior1)
        val param1 = newSavedParamFor(version1)
        val behavior2 = newSavedBehaviorFor(group)
        val version2 = newSavedVersionFor(behavior2)
        val param2 = newSavedParamFor(version2)

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head

        mustBeValidImport(exportedGroup, importedGroup)

        val importedSharedInputs = runNow(dataService.inputs.allForGroup(importedGroup))
        importedSharedInputs must have length 0
        val importedBehaviors = runNow(dataService.behaviors.allForGroup(importedGroup))
        val importedVersions = runNow(Future.sequence(importedBehaviors.map { behavior =>
          dataService.behaviors.maybeCurrentVersionFor(behavior)
        }).map(_.flatten))
        importedVersions must have length 2
        val importedParams = runNow(Future.sequence(importedVersions.map { version =>
          dataService.behaviorParameters.allFor(version)
        }).map(_.flatten))
        importedParams must have length 2
        importedParams.head.input must not be importedParams.tail.head.input
      })
    }

    "export and import back in with shared param" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior1 = newSavedBehaviorFor(group)
        val version1 = newSavedVersionFor(behavior1)
        val param1 = newSavedParamFor(version1, isSavedForTeam = Some(true))
        val behavior2 = newSavedBehaviorFor(group)
        val version2 = newSavedVersionFor(behavior2)
        val param2 = newSavedParamFor(version2, maybeExistingInput = Some(param1.input))

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head

        mustBeValidImport(exportedGroup, importedGroup)

        val importedInputs = runNow(dataService.inputs.allForGroup(importedGroup))
        importedInputs must have length 1
        val importedBehaviors = runNow(dataService.behaviors.allForGroup(importedGroup))
        val importedVersions = runNow(Future.sequence(importedBehaviors.map { behavior =>
          dataService.behaviors.maybeCurrentVersionFor(behavior)
        }).map(_.flatten))
        importedVersions must have length 2
        val importedParams = runNow(Future.sequence(importedVersions.map { version =>
          dataService.behaviorParameters.allFor(version)
        }).map(_.flatten))
        importedParams must have length 2
        importedParams.head.input mustBe importedParams.tail.head.input
      })
    }

    "export and import back in with a data type" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val dataType = newSavedDataTypeFor(group)
        val dataTypeVersion = newSavedVersionFor(dataType)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val paramType = BehaviorBackedDataType(dataType)
        val dataTypeParamTypeData = runNow(BehaviorParameterTypeData.from(paramType, dataService))
        val param = newSavedParamFor(version, maybeType = Some(dataTypeParamTypeData))

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head

        mustBeValidImport(exportedGroup, importedGroup)

        val importedSharedInputs = runNow(dataService.inputs.allForGroup(importedGroup))
        importedSharedInputs must have length 0
        val importedDataTypes = runNow(dataService.behaviors.dataTypesForGroup(importedGroup))
        importedDataTypes must have length 1
        val importedBehaviors = runNow(dataService.behaviors.regularForGroup(importedGroup))
        importedBehaviors must have length 1
        val importedVersions = runNow(Future.sequence(importedBehaviors.map { behavior =>
          dataService.behaviors.maybeCurrentVersionFor(behavior)
        }).map(_.flatten))
        importedVersions must have length 1
        val importedParams = runNow(Future.sequence(importedVersions.map { version =>
          dataService.behaviorParameters.allFor(version)
        }).map(_.flatten))
        importedParams must have length 1
        importedParams.head.input.paramType.id mustBe importedDataTypes.head.id
      })
    }


  }

}
