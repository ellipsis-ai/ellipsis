import export.{BehaviorGroupExporter, BehaviorGroupZipImporter}
import models.behaviors.behaviorgroup.BehaviorGroup
import support.DBSpec

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
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        runNow(dataService.behaviorVersions.save(version))
        val param = newSavedParamFor(version, isSavedForTeam = Some(true))
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
      })
    }

  }

}
