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

  def dashboard: NavItem = {
    NavItem("Dashboard", None)
  }

  def dashboardUsage(link: Boolean, maybeTeamId: Option[String]): NavItem = {
    NavItem("Usage report", if (link) {
      Some(controllers.routes.DashboardController.usage(maybeTeamId))
    } else {
      None
    })
  }

  def dashboardSkillManifest(link: Boolean, maybeTeamId: Option[String]): NavItem = {
    NavItem("Skill manifest", if (link) {
      Some(controllers.routes.DashboardController.skillManifest(maybeTeamId))
    } else {
      None
    })
  }

  def scheduling(link: Boolean, maybeTeamId: Option[String]): NavItem = {
    NavItem("Scheduling", if (link) {
      Some(controllers.routes.ScheduledActionsController.index(
        selectedId = None,
        newSchedule = None,
        channelId = None,
        skillId = None,
        teamId = maybeTeamId,
        forceAdmin = None
      ))
    } else {
      None
    })
  }

  def skillConfigFor(skillName: String): NavItem = {
    NavItem(skillName, None)
  }

  def skillScheduling: NavItem = {
    NavItem("Scheduling", None)
  }

  def teamSettings: NavItem = {
    NavItem("Settings", None)
  }

  def help: NavItem = {
    NavItem("Help", None)
  }

  def admin: NavItem = {
    NavItem("Admin", None)
  }

  def adminTeamsList(link: Boolean): NavItem = {
    NavItem("Teams admin", if (link) {
      Some(controllers.admin.routes.TeamsController.list(0, 10, None))
    } else {
      None
    })
  }

  def integrations(link: Boolean, maybeTeamId: Option[String]) = {
    NavItem("Integrations", if (link) {
      Some(controllers.web.settings.routes.IntegrationsController.list(maybeTeamId))
    } else {
      None
    })
  }

}
