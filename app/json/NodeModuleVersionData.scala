package json

import models.behaviors.nodemoduleversion.NodeModuleVersion

case class NodeModuleVersionData(from: String, version: String)

object NodeModuleVersionData {

  def from(version: NodeModuleVersion) = NodeModuleVersionData(version.name, version.version)

}
