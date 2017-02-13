package models.behaviors.behaviorgroup

import java.time.OffsetDateTime

import models.team.Team
import utils.SafeFileName

case class BehaviorGroup(
                          id: String,
                          name: String,
                          maybeIcon: Option[String],
                          maybeDescription: Option[String],
                          maybeImportedId: Option[String],
                          team: Team,
                          createdAt: OffsetDateTime) {

  def exportName: String = {
    Option(SafeFileName.forName(name)).filter(_.nonEmpty).getOrElse(id)
  }

  def toRaw: RawBehaviorGroup = RawBehaviorGroup(
    id,
    name,
    maybeIcon,
    maybeDescription,
    maybeImportedId,
    team.id,
    createdAt
  )


}
