import drivers.SlickPostgresDriver.api._
import export.{BehaviorGroupExporter, BehaviorGroupZipImporter}
import json.{BehaviorParameterTypeData, BehaviorVersionData, LibraryVersionData}
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.{BehaviorGroup, BehaviorGroupQueries}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorBackedDataType, BehaviorParameterType}
import models.behaviors.behaviorversion.BehaviorVersion
import support.DBSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorGroupImportExportSpec extends DBSpec {

  def checkParamTypeMatches(paramType: BehaviorParameterType, dataType: BehaviorVersion): Unit = {
    paramType.isInstanceOf[BehaviorBackedDataType] mustBe true
    dataType.behavior.maybeExportId must contain(paramType.exportId)
    dataType.id mustBe paramType.id
    dataType.behavior.isDataType mustBe true
    dataType.maybeName must contain(paramType.name)
  }

  def exportAndImport(group: BehaviorGroup, exportUser: User, importUser: User): Unit = {
    val maybeImportTeam = runNow(dataService.teams.find(importUser.teamId))
    val maybeExporter = runNow(BehaviorGroupExporter.maybeFor(group.id, exportUser, dataService))
    maybeExporter.isDefined mustBe(true)
    val file = maybeExporter.get.getZipFile

    // change the existing export ID so it's not a re-install
    runNow(dataService.run(BehaviorGroupQueries.all.filter(_.id === group.id).map(_.maybeExportId).update(Some(IDs.next))))

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

  def checkImportedInputsCorrectly(exportedVersion: BehaviorGroupVersion, importedVersion: BehaviorGroupVersion): Unit = {
    val exportedInputs = runNow(dataService.inputs.allForGroupVersion(exportedVersion))
    val importedInputs = runNow(dataService.inputs.allForGroupVersion(importedVersion))
    exportedInputs.length mustBe importedInputs.length
    exportedInputs.map(_.id).intersect(importedInputs.map(_.id)) mustBe empty
  }

  def mustBeValidImport(exported: BehaviorGroup, imported: BehaviorGroup): Unit = {
    val reloadedExported = runNow(dataService.behaviorGroups.find(exported.id).map(_.get))
    reloadedExported.id must not be(imported.id)

    // since we changed the existing export ID
    imported.maybeExportId must not be(reloadedExported.maybeExportId)

    val exportedVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(exported)).get
    val importedVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(imported)).get

    checkImportedBehaviorsCorrectly(reloadedExported, imported)
    checkImportedDataTypesCorrectly(reloadedExported, imported)
    checkImportedInputsCorrectly(exportedVersion, importedVersion)
  }

  "BehaviorGroupExporter" should {

    "export and import back in" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        newSavedGroupVersionFor(group, user)

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head
        val importedGroupVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(importedGroup)).get

        mustBeValidImport(exportedGroup, importedGroup)

        val importedInputs = runNow(dataService.inputs.allForGroupVersion(importedGroupVersion))
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

        val input1Data = newInputDataFor(isSavedForTeam = Some(true))
        val behaviorVersion1Data = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(input1Data.inputId.get)
        )
        val behaviorVersion2Data = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(input1Data.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersion1Data, behaviorVersion2Data),
          actionInputs = Seq(input1Data)
        )
        newSavedGroupVersionFor(group, user, Some(groupData))

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head
        val importedGroupVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(importedGroup)).get

        mustBeValidImport(exportedGroup, importedGroup)

        val importedInputs = runNow(dataService.inputs.allForGroupVersion(importedGroupVersion))
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

        val dataTypeVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = true, dataService).copy(name = Some("A data type"))

        val dataTypeParamData = BehaviorParameterTypeData(
          dataTypeVersionData.id,
          dataTypeVersionData.exportId,
          dataTypeVersionData.name.get,
          None
        )

        val inputData = newInputDataFor(Some(dataTypeParamData))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(dataTypeVersionData, behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        newSavedGroupVersionFor(group, user, Some(groupData))

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head

        mustBeValidImport(exportedGroup, importedGroup)

        val importedGroupVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(importedGroup)).get
        val importedInputs = runNow(dataService.inputs.allForGroupVersion(importedGroupVersion))
        importedInputs must have length 1
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
        val importedDataTypes = runNow(dataService.behaviors.dataTypesForGroup(importedGroup))
        val importedDataTypeVersions = runNow(Future.sequence(importedDataTypes.map { behavior =>
          dataService.behaviors.maybeCurrentVersionFor(behavior)
        }).map(_.flatten))
        importedDataTypeVersions must have length 1
        checkParamTypeMatches(importedParams.head.input.paramType, importedDataTypeVersions.head)
      })
    }

    "export and import back in with a search data type" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor()

        val dataTypeVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = true, dataService).copy(
          name = Some("A data type"),
          inputIds = Seq(inputData.inputId.get)
        )

        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(dataTypeVersionData),
          dataTypeInputs = Seq(inputData)
        )
        newSavedGroupVersionFor(group, user, Some(groupData))

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head

        mustBeValidImport(exportedGroup, importedGroup)

        val importedGroupVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(importedGroup)).get
        val importedInputs = runNow(dataService.inputs.allForGroupVersion(importedGroupVersion))
        importedInputs must have length 1
        val importedDataTypes = runNow(dataService.behaviors.dataTypesForGroup(importedGroup))
        importedDataTypes must have length 1
        val importedDataTypeVersions = runNow(Future.sequence(importedDataTypes.map { behavior =>
          dataService.behaviors.maybeCurrentVersionFor(behavior)
        }).map(_.flatten))
        importedDataTypeVersions must have length 1
        val importedParams = runNow(Future.sequence(importedDataTypeVersions.map { version =>
          dataService.behaviorParameters.allFor(version)
        }).map(_.flatten))
        importedParams must have length 1
      })
    }

    "export and import back in with a code library" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService)
        val libraryName = IDs.next
        val libraryDescription = "A description"
        val libraryVersionData = LibraryVersionData.newUnsaved.copy(name = libraryName, description = Some(libraryDescription))

        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          libraryVersions = Seq(libraryVersionData)
        )

        newSavedGroupVersionFor(group, user, Some(groupData))

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        exportAndImport(group, user, user)

        val groupsAfter = runNow(dataService.behaviorGroups.allFor(team))
        groupsAfter must have length 2

        val exportedGroup = groupsBefore.head
        val importedGroup = groupsAfter.filterNot(_.id == exportedGroup.id).head
        val importedGroupVersion = runNow(dataService.behaviorGroups.maybeCurrentVersionFor(importedGroup)).get

        mustBeValidImport(exportedGroup, importedGroup)

        val importedCodeLibraries = runNow(dataService.libraries.allFor(importedGroupVersion))
        importedCodeLibraries must have length 1
        val lib = importedCodeLibraries.head
        lib.maybeExportId.isDefined mustBe true
        lib.name mustBe libraryName
        lib.maybeDescription must contain(libraryDescription)
      })
    }



  }

}
