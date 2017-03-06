package models.behaviors.behaviorgroup

import java.time.OffsetDateTime

import models.team.Team

case class BehaviorGroup(
                          id: String,
                          maybeExportId: Option[String],
                          team: Team,
                          maybeCurrentVersionId: Option[String],
                          createdAt: OffsetDateTime) {

  def toRaw: RawBehaviorGroup = RawBehaviorGroup(
    id,
    maybeExportId,
    team.id,
    maybeCurrentVersionId,
    createdAt
  )


}
