package models.behaviors.behavior

import java.time.OffsetDateTime

import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team

case class Behavior(
                     id: String,
                     team: Team,
                     maybeGroup: Option[BehaviorGroup],
                     maybeExportId: Option[String],
                     maybeDataTypeName: Option[String],
                     createdAt: OffsetDateTime
                   ) {

  def group: BehaviorGroup = maybeGroup.get

  val isDataType = maybeDataTypeName.isDefined

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeGroup.map(_.id), maybeExportId, maybeDataTypeName, createdAt)
  }

}
