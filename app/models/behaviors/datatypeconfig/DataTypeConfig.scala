package models.behaviors.datatypeconfig

import models.behaviors.behaviorversion.BehaviorVersion

case class DataTypeConfig(
                          id: String,
                          maybeUsesCode: Option[Boolean],
                          behaviorVersion: BehaviorVersion
                        ) {

  def usesCode: Boolean = maybeUsesCode.isEmpty || maybeUsesCode.contains(true)

  def toRaw: RawDataTypeConfig = RawDataTypeConfig(id, maybeUsesCode, behaviorVersion.id)
}
