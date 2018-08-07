package models.behaviors.behavior

import java.time.OffsetDateTime

import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team

case class Behavior(
                     id: String,
                     team: Team,
                     group: BehaviorGroup,
                     maybeExportId: Option[String],
                     isDataType: Boolean,
                     createdAt: OffsetDateTime
                   ) {

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, group.id, maybeExportId, isDataType, createdAt)
  }

}
