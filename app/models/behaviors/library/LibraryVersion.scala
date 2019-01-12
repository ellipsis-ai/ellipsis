package models.behaviors.library

import java.time.OffsetDateTime

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class LibraryVersion(
                           id: String,
                           libraryId: String,
                           maybeExportId: Option[String],
                           name: String,
                           maybeDescription: Option[String],
                           functionBody: String,
                           groupVersion: BehaviorGroupVersion,
                           createdAt: OffsetDateTime
                          ) {

  val jsName = s"$name.js"

  val code = LibraryVersion.codeFor(functionBody)

  def toRaw: RawLibraryVersion = RawLibraryVersion(
    id,
    libraryId,
    maybeExportId,
    name,
    maybeDescription,
    functionBody,
    groupVersion.id,
    createdAt
  )
}

object LibraryVersion {

  def codeFor(functionBody: String): String = {
    s"""module.exports = (function() {
         |$functionBody
         |})()
     """.stripMargin
  }

  val functionBodyRegex = """(?s)\s*module\.exports\s*=\s*\(function\(\)\s*\{\s*(.*)\n\}\)\(\)""".r

  def functionBodyFrom(code: String): String = {
    functionBodyRegex.findFirstMatchIn(code).map { firstMatch =>
      firstMatch.subgroups(0)
    }.getOrElse(code)
  }

}
