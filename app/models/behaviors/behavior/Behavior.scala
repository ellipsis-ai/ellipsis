package models.behaviors.behavior

import models.team.Team
import org.joda.time.DateTime
import play.api.Configuration

case class Behavior(
                     id: String,
                     team: Team,
                     maybeCurrentVersionId: Option[String],
                     maybeImportedId: Option[String],
                     maybeDataTypeName: Option[String],
                     createdAt: DateTime
                   ) {

  def editLinkFor(configuration: Configuration): String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val path = controllers.routes.BehaviorEditorController.edit(id)
    s"$baseUrl$path"
  }

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeCurrentVersionId, maybeImportedId, maybeDataTypeName, createdAt)
  }

}
