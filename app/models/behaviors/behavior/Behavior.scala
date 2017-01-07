package models.behaviors.behavior

import java.time.ZonedDateTime

import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team

case class Behavior(
                     id: String,
                     team: Team,
                     maybeGroup: Option[BehaviorGroup],
                     maybeCurrentVersionId: Option[String],
                     maybeImportedId: Option[String],
                     maybeDataTypeName: Option[String],
                     createdAt: ZonedDateTime
                   ) {

  val isDataType = maybeDataTypeName.isDefined

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeGroup.map(_.id), maybeCurrentVersionId, maybeImportedId, maybeDataTypeName, createdAt)
  }

}
