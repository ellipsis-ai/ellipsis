package models.behaviors.behaviorgroup

import models.team.Team
import org.joda.time.DateTime
import utils.SafeFileName

case class BehaviorGroup(
                          id: String,
                          name: String,
                          maybeDescription: Option[String],
                          maybeImportedId: Option[String],
                          team: Team,
                          createdAt: DateTime) {

  def exportName: String = {
    Option(SafeFileName.forName(name)).filter(_.nonEmpty).getOrElse(id)
  }

  def toRaw: RawBehaviorGroup = RawBehaviorGroup(
    id,
    name,
    maybeDescription,
    maybeImportedId,
    team.id,
    createdAt
  )


}
