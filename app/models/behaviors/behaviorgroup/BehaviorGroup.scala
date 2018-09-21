package models.behaviors.behaviorgroup

import java.time.OffsetDateTime

import models.team.Team

case class BehaviorGroup(
                          id: String,
                          maybeExportId: Option[String],
                          team: Team,
                          createdAt: OffsetDateTime
                        ) {

  def toRaw: RawBehaviorGroup = RawBehaviorGroup(
    id,
    maybeExportId,
    team.id,
    createdAt,
    None
  )


}
