package models.behaviors.builtins.admin

import models.behaviors.builtins.BuiltinBehavior
import services.DataService

trait BuiltinAdminBehavior extends BuiltinBehavior {
  lazy val dataService: DataService = services.dataService

  protected def teamLinkFor(teamId: String): String = {
    val baseUrl = services.lambdaService.configuration.get[String]("application.apiBaseUrl")
    s"${baseUrl}${controllers.routes.ApplicationController.index(Some(teamId)).url}"
  }
}
