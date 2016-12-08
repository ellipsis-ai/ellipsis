package models.behaviors.behaviorgroup

import models.team.Team
import org.joda.time.LocalDateTime

case class BehaviorGroup(
                          id: String,
                          name: String,
                          maybeDescription: Option[String],
                          maybeImportedId: Option[String],
                          team: Team,
                          createdAt: LocalDateTime) {

  def exportName: String = {
    val safeName = name.trim.replaceAll("""\s""", "_").replaceAll("""[^A-Za-z0-9_-]""", "")
    Option(safeName).filter(_.nonEmpty).getOrElse(id)
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
