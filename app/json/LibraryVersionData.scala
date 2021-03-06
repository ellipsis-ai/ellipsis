package json

import java.time.OffsetDateTime

import export.BehaviorGroupExporter
import models.IDs
import models.behaviors.library.LibraryVersion
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class LibraryVersionData(
                               id: Option[String],
                               libraryId: Option[String],
                               exportId: Option[String],
                               isNew: Option[Boolean],
                               name: String,
                               description: Option[String],
                               functionBody: String,
                               createdAt: OffsetDateTime
                            ) {

  val code: String = LibraryVersion.codeFor(functionBody)

  def maybeExportName: Option[String] = {
    Option(name).filter(_.trim.nonEmpty).orElse(exportId).map(_ ++ ".js")
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): LibraryVersionData = {
    copy(
      id = None,
      libraryId = None
    )
  }

  def copyWithIdsEnsuredFor(maybeExistingGroupData: Option[BehaviorGroupData]): LibraryVersionData = {
    val maybeExisting = maybeExistingGroupData.flatMap { data =>
      data.libraryVersions.find(_.exportId == exportId)
    }
    copy(
      id = id.orElse(Some(IDs.next)),
      libraryId = maybeExisting.flatMap(_.libraryId).orElse(libraryId).orElse(Some(IDs.next))
    )
  }

  def copyWithNewIdIn(oldToNewIdMapping: collection.mutable.Map[String, String]): LibraryVersionData = {
    val newId = IDs.next
    val maybeOldID = id
    maybeOldID.foreach { oldId => oldToNewIdMapping.put(oldId, newId) }
    copy(id = Some(newId))
  }

  def exportFileContent: String = {
    val descriptionText = description.filter(_.trim.nonEmpty).map(_ ++ "\n").getOrElse("")
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
    LibraryVersionData(
      Some(version.id),
      Some(version.libraryId),
      version.maybeExportId,
      isNew = Some(false),
      version.name,
      version.maybeDescription,
      version.functionBody,
      version.createdAt
    )
  }

  def newUnsaved: LibraryVersionData = LibraryVersionData(
    id = Some(IDs.next),
    libraryId = Some(IDs.next),
    exportId = Some(IDs.next),
    isNew = Some(true),
    name = "",
    description = None,
    functionBody = "",
    OffsetDateTime.now
  )

  def maybeClonedFor(libraryIdToClone: String, dataService: DataService)(implicit ec: ExecutionContext): Future[Option[LibraryVersionData]] = {
    for {
      maybeExisting <- dataService.libraries.findCurrentByLibraryId(libraryIdToClone)
    } yield {
      maybeExisting.map { existing =>
        LibraryVersionData(
          id = Some(IDs.next),
          libraryId = Some(IDs.next),
          exportId = Some(IDs.next),
          isNew = Some(true),
          name = s"${existing.name}-copy",
          description = existing.maybeDescription,
          functionBody = existing.functionBody,
          createdAt = OffsetDateTime.now
        )
      }
    }
  }

  val libNameRegex = """^(.+)\.js""".r
  val libContentRegex = """(?s)\/\*\s*([^$]*)\s+@exportId\s+(\S+)\s*\*\/\s*(.*)""".r

  def from(content: String, filename: String): LibraryVersionData = {
    var maybeExportId: Option[String] = None
    var maybeDescription: Option[String] = None
    var code: String = ""
    libContentRegex.findFirstMatchIn(content).foreach { firstMatch =>
      maybeDescription = Some(firstMatch.subgroups(0))
      maybeExportId = Some(firstMatch.subgroups(1))
      code = firstMatch.subgroups(2)
    }
    val name = libNameRegex.findFirstMatchIn(filename).map { firstMatch =>
      firstMatch.subgroups(0)
    }.getOrElse(filename)
    LibraryVersionData(
      None,
      None,
      maybeExportId,
      isNew = Some(false),
      name,
      maybeDescription,
      LibraryVersion.functionBodyFrom(code),
      OffsetDateTime.now
    )
  }
}
