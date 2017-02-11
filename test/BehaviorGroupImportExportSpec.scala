import export.{BehaviorGroupExporter, BehaviorGroupZipImporter}
import models.behaviors.behaviorgroup.BehaviorGroup
import support.DBSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorGroupImportExportSpec extends DBSpec {

  def mustBeValidImport(exported: BehaviorGroup, imported: BehaviorGroup): Unit = {
    exported.id must not be(imported.id)
    imported.maybeImportedId must contain(exported.id)
    val exportedBehaviors = runNow(dataService.behaviors.allForGroup(exported))
    val importedBehaviors = runNow(dataService.behaviors.allForGroup(imported))
    exportedBehaviors.length mustBe importedBehaviors.length

    val exportedInputs = runNow(dataService.inputs.allForGroup(exported))
    val importedInputs = runNow(dataService.inputs.allForGroup(imported))
    exportedInputs.length mustBe importedInputs.length
    exportedInputs.map(_.id).intersect(importedInputs.map(_.id)) mustBe empty
  }

  "BehaviorGroupExporter" should {

    "export and import back in" in {
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
        val maybeExporter = runNow(BehaviorGroupExporter.maybeFor(group.id, user, dataService))
        maybeExporter.isDefined mustBe(true)
        val file = maybeExporter.get.getZipFile
        val importer = BehaviorGroupZipImporter(team, user, file, dataService)

        val groupsBefore = runNow(dataService.behaviorGroups.allFor(team))
        groupsBefore must have length 1

        runNow(importer.run)

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

  }

}
