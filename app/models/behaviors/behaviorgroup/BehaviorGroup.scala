package models.behaviors.behaviorgroup

import java.time.OffsetDateTime

import models.team.Team
import utils.SafeFileName

case class BehaviorGroup(
                          id: String,
                          name: String,
                          maybeIcon: Option[String],
                          maybeDescription: Option[String],
                          maybeExportId: Option[String],
                          team: Team,
                          maybeCurrentVersionId: Option[String],
                          createdAt: OffsetDateTime) {

  def exportName: String = {
    Option(SafeFileName.forName(name)).filter(_.nonEmpty).getOrElse(id)
  }

  def toRaw: RawBehaviorGroup = RawBehaviorGroup(
    id,
    name,
    maybeIcon,
    maybeDescription,
    maybeExportId,
    team.id,
    maybeCurrentVersionId,
    createdAt
  )


}
