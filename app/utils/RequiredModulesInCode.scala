package utils

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.library.LibraryVersion

object RequiredModulesInCode {

  val requireRegex = """.*require\s*\(['"]\s*(?:\.\/)?(\S+)\s*['"]\).*""".r
  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  def requiredModulesIn(code: String, libraries: Seq[LibraryVersion], includeLibraryRequires: Boolean): Seq[String] = {
    val libraryNames = libraries.map(_.name)
    val requiredForCode =
      requireRegex.findAllMatchIn(code).
        flatMap(_.subgroups.headOption).
        toArray.
        diff(alreadyIncludedModules ++ libraryNames).
        sorted
    val requiredForLibs = if (includeLibraryRequires) {
      libraries.flatMap(ea => requiredModulesIn(ea.functionBody, libraries, includeLibraryRequires = false))
    } else {
      Seq()
    }
    (requiredForCode ++ requiredForLibs).distinct
  }

  def requiredModulesIn(behaviorVersions: Seq[BehaviorVersion], libraries: Seq[LibraryVersion], includeLibraryRequires: Boolean): Array[String] = {
    behaviorVersions.flatMap { ea =>
      requiredModulesIn(ea.functionBody, libraries, includeLibraryRequires)
    }.distinct.toArray
  }

}
