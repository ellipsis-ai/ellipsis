package models.behaviors.library

import java.time.OffsetDateTime

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

case class LibraryVersion(
                            id: String,
                            name: String,
                            code: String,
                            behaviorGroupVersionId: String,
                            createdAt: OffsetDateTime
                          ) {

  val jsName = s"$name.js"

}
