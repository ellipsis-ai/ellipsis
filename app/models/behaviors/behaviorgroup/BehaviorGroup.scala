package models.behaviors.behaviorgroup

import models.team.Team
import org.joda.time.DateTime

case class BehaviorGroup(
                          id: String,
                          name: String,
                          maybeImportedId: Option[String],
                          team: Team,
                          createdAt: DateTime) {
  // TODO: want this in model for real
  val maybeDescription = Some(name)
}
