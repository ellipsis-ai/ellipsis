package models

import play.api.mvc.Call

case class NavItem(title: String, route: Option[Call])

object NavItem {

  def skills(maybeTeamId: Option[String], maybeBranch: Option[String]): NavItem = {
    NavItem("Skills", Some(controllers.routes.ApplicationController.index(maybeTeamId, maybeBranch)))
  }

  def scheduling(maybeTeamId: Option[String]): NavItem = {
    NavItem("Scheduling", Some(controllers.routes.ScheduledActionsController.index(None, None, maybeTeamId)))
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
