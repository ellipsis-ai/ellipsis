package models.behaviors.ellipsisobject

import json.LibraryVersionData

case class LibraryInfo(
                        name: String,
                        functionBody: String
                      )

object LibraryInfo {

  def allFor(versions: Seq[LibraryVersionData]): Seq[LibraryInfo] = {
    versions.map { ea =>
      LibraryInfo(ea.name, ea.functionBody)
    }
  }

}
