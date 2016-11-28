package models.behaviors.behaviorgroup

import models.team.Team
import org.joda.time.DateTime

case class BehaviorGroup(
                          id: String,
                          name: String,
                          maybeDescription: Option[String],
                          maybeImportedId: Option[String],
                          team: Team,
                          createdAt: DateTime) {

  def exportName: String = {
    Option(name.trim).filter(_.nonEmpty).getOrElse(id)
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
