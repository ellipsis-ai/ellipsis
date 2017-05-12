package json

import export.BehaviorGroupExporter
import models.IDs
import models.behaviors.library.LibraryVersion

case class LibraryVersionData(
                               id: Option[String],
                               libraryId: Option[String],
                               exportId: Option[String],
                               name: String,
                               description: Option[String],
                               functionBody: String
                            ) {

  val code =
    s"""module.exports = (function() {
       |  $functionBody
       |})()
     """.stripMargin

  def maybeExportName: Option[String] = {
    Option(name).filter(_.trim.nonEmpty).orElse(exportId).map(_ ++ ".js")
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): LibraryVersionData = {
    copy(
      id = None,
      libraryId = None
    )
  }

  def exportFileContent: String = {
    val descriptionText = description.map(_ ++ "\n").getOrElse("")
    val exportIdText = exportId.map { id => s"@exportId $id\n" }.getOrElse("")
    val headerContent = if (descriptionText.isEmpty && exportIdText.isEmpty) {
      ""
    } else {
      s"/*\n$descriptionText$exportIdText*/\n"
    }
    headerContent ++ code
  }

}

object LibraryVersionData {

  def fromVersion(version: LibraryVersion): LibraryVersionData = {
    LibraryVersionData(Some(version.id), Some(version.libraryId), version.maybeExportId, version.name, version.maybeDescription, version.functionBody)
  }

  def newUnsaved: LibraryVersionData = LibraryVersionData(
    id = Some(IDs.next),
    libraryId = Some(IDs.next),
    exportId = Some(IDs.next),
    name = "",
    description = None,
    functionBody = ""
  )
}
