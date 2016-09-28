package models.behaviors.behavior

import models.team.Team
import org.joda.time.DateTime

case class Behavior(
                     id: String,
                     team: Team,
                     maybeCurrentVersionId: Option[String],
                     maybeImportedId: Option[String],
                     createdAt: DateTime
                   ) {

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeCurrentVersionId, maybeImportedId, createdAt)
  }

}
