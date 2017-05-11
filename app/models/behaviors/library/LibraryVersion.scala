package models.behaviors.library

import java.time.OffsetDateTime

case class LibraryVersion(
                            id: String,
                            libraryId: String,
                            name: String,
                            code: String,
                            behaviorGroupVersionId: String,
                            createdAt: OffsetDateTime
                          ) {

  val jsName = s"$name.js"

}
