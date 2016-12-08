package models.behaviors.behavior

import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import org.joda.time.LocalDateTime
import play.api.Configuration

case class Behavior(
                     id: String,
                     team: Team,
                     maybeGroup: Option[BehaviorGroup],
                     maybeCurrentVersionId: Option[String],
                     maybeImportedId: Option[String],
                     maybeDataTypeName: Option[String],
                     createdAt: LocalDateTime
                   ) {

  val isDataType = maybeDataTypeName.isDefined

  def editLinkFor(configuration: Configuration): String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val path = controllers.routes.BehaviorEditorController.edit(id)
    s"$baseUrl$path"
  }

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeGroup.map(_.id), maybeCurrentVersionId, maybeImportedId, maybeDataTypeName, createdAt)
  }

}
