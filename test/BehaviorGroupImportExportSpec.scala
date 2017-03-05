import export.{BehaviorGroupExporter, BehaviorGroupZipImporter}
import json.BehaviorParameterTypeData
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType}
import support.DBSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorGroupImportExportSpec extends DBSpec {

  def checkParamTypeMatches(paramType: BehaviorParameterType, dataType: Behavior): Unit = {
    paramType.isInstanceOf[BehaviorBackedDataType] mustBe true
    dataType.maybeExportId must contain(paramType.exportId)
    dataType.maybeDataTypeName must contain(paramType.name)
  }

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
    val reloadedExported = runNow(dataService.behaviorGroups.find(exported.id).map(_.get))
    reloadedExported.id must not be(imported.id)
    imported.maybeExportId mustBe reloadedExported.maybeExportId

    checkImportedBehaviorsCorrectly(reloadedExported, imported)
    checkImportedDataTypesCorrectly(reloadedExported, imported)
    checkImportedInputsCorrectly(reloadedExported, imported)
  }

  "BehaviorGroupExporter" should {

    "export and import back in" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior1 = newSavedBehaviorFor(group)
        val behavior2 = newSavedBehaviorFor(group)
        val groupVersion = newSavedGroupVersionFor(group, user)
        val version1 = behaviorVersionFor(behavior1, groupVersion)
        val version2 = behaviorVersionFor(behavior2, groupVersion)
        val param1 = newSavedParamFor(version1)
        val param2 = newSavedParamFor(version2)

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head

        mustBeValidImport(exportedGroup, importedGroup)

        val importedInputs = runNow(dataService.inputs.allForGroup(importedGroup))
        importedInputs must have length 2
        importedInputs.head.id mustNot be(importedInputs.tail.head.id)
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
        val behavior2 = newSavedBehaviorFor(group)
        val groupVersion = newSavedGroupVersionFor(group, user)
        val version1 = behaviorVersionFor(behavior1, groupVersion)
        val version2 = behaviorVersionFor(behavior2, groupVersion)
        val param1 = newSavedParamFor(version1, isSavedForTeam = Some(true))
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
        val behavior = newSavedBehaviorFor(group)
        val groupVersion = newSavedGroupVersionFor(group, user)
        val dataTypeVersion = behaviorVersionFor(dataType, groupVersion)
        val version = behaviorVersionFor(behavior, groupVersion)
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

        val importedInputs = runNow(dataService.inputs.allForGroup(importedGroup))
        importedInputs must have length 1
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
        checkParamTypeMatches(importedParams.head.input.paramType, importedDataTypes.head)
      })
    }


  }

}
