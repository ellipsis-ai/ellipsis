package models.behaviors.builtins.admin

import models.behaviors.builtins.BuiltinBehavior
import services.DataService

trait BuiltinAdminBehavior extends BuiltinBehavior {
  lazy val dataService: DataService = services.dataService
  lazy val baseUrl: String = services.configuration.get[String]("application.apiBaseUrl")

  protected def teamLinkFor(teamId: String): String = {
    s"${baseUrl}${controllers.routes.ApplicationController.index(Some(teamId)).url}"
  }

  protected def editSkillLinkFor(groupId: String, maybeEditableId: Option[String] = None): String = {
    s"$baseUrl${controllers.routes.BehaviorEditorController.edit(groupId, maybeEditableId)}"
  }
}
