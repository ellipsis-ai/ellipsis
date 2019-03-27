package models.behaviors.ellipsisobject

import models.behaviors.events.Event
import play.api.Configuration

case class ScheduleInfo(editLink: String, recurrence: String)

object ScheduleInfo {
  def maybeBuildFor(event: Event, configuration: Configuration): Option[ScheduleInfo] = {
    event.maybeScheduled.map { scheduled =>
      val baseUrl = configuration.get[String]("application.apiBaseUrl")
      val editUrl = controllers.routes.ScheduledActionsController.index(
        selectedId = Some(scheduled.id),
        newSchedule = None,
        channelId = scheduled.maybeChannel,
        teamId = Some(event.ellipsisTeamId),
        forceAdmin = None
      ).url
      val editLink = baseUrl + editUrl
      ScheduleInfo(editLink, scheduled.recurrence.displayString)
    }
  }
}
