package models.behaviors.nodemoduleversion

case class NodeModuleVersion(
                              id: String,
                              name: String,
                              version: String,
                              groupVersionId: String
                            ) {
  val nameWithoutVersion: String = {
    name.replaceFirst("""@.+$""", "")
  }
}
