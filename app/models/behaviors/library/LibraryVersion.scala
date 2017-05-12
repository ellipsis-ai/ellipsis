package models.behaviors.library

import java.time.OffsetDateTime

case class LibraryVersion(
                            id: String,
                            libraryId: String,
                            name: String,
                            maybeDescription: Option[String],
                            functionBody: String,
                            behaviorGroupVersionId: String,
                            createdAt: OffsetDateTime
                          ) {

  val jsName = s"$name.js"

  val code =
    s"""module.exports = (function() {
       |  $functionBody
       |})()
     """.stripMargin

}
