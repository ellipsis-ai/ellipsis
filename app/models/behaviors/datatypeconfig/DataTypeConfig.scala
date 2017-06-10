package models.behaviors.datatypeconfig

import models.behaviors.behaviorversion.BehaviorVersion


case class DataTypeConfig(
                          id: String,
                          behaviorVersion: BehaviorVersion
                        ) {

  lazy val name = behaviorVersion.maybeName.getOrElse("Unnamed type")

  def toRaw: RawDataTypeConfig = RawDataTypeConfig(id, behaviorVersion.id)
}
