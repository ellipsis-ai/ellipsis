package json

import models.IDs
import models.behaviors.library.LibraryVersion

case class LibraryVersionData(
                               id: String,
                               libraryId: String,
                               name: String,
                               code: String
                            ) {

}

object LibraryVersionData {

  def fromVersion(version: LibraryVersion): LibraryVersionData = {
    LibraryVersionData(version.id, version.libraryId, version.name, version.code)
  }

  def newUnsaved: LibraryVersionData = LibraryVersionData(
    id = IDs.next,
    libraryId = IDs.next,
    name = "",
    code = ""
  )
}
