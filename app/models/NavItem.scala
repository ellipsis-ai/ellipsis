package models

import play.api.mvc.Call

case class NavItem(title: String, maybeRoute: Option[Call])

object NavItem {

  def skills(link: Boolean, maybeTeamId: Option[String], maybeBranch: Option[String]): NavItem = {
    NavItem("Skills", if (link) {
      Some(controllers.routes.ApplicationController.index(maybeTeamId, maybeBranch))
    } else {
      None
    })
  }

  def scheduling(link: Boolean, maybeTeamId: Option[String]): NavItem = {
    NavItem("Scheduling", if (link) {
      Some(controllers.routes.ScheduledActionsController.index(None, None, maybeTeamId))
    } else {
      None
    })

  }

  def teamSettings(maybeTeamId: Option[String]): NavItem = {
    NavItem("Settings", Some(controllers.web.settings.routes.RegionalSettingsController.index(maybeTeamId)))
  }

  def help: NavItem = {
    NavItem("Help", None)
  }

  def admin: NavItem = {
    NavItem("Admin", None)
  }

}
