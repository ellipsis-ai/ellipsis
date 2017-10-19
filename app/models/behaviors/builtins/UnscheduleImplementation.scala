package models.behaviors.builtins

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import models.team.Team
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class UnscheduleImplementation(
                               text: String,
                               event: Event,
                               services: DefaultServices
                             ) extends BuiltinImplementation {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val configuration = services.configuration
    val dataService = services.dataService
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      response <- (for {
        team <- maybeTeam
        channel <- event.maybeChannel
      } yield {
        for {
          scheduledMessages <- dataService.scheduledMessages.allForText(text, team, None, Some(channel))
          deleted <- Future.sequence(scheduledMessages.map { scheduled =>
            dataService.scheduledMessages.delete(scheduled)
          }).map(_.flatten)
          scheduledInChannel <- dataService.scheduledMessages.allForChannel(team, channel)
          listResponses <- Future.sequence(scheduledInChannel.map(ea => ea.listResponse(ea.id, ea.team.id, dataService, configuration, includeChannel = false)))
        } yield {
          if (deleted.nonEmpty) {
            if (deleted.length > 1) {
              s"OK, I unscheduled the ${deleted.length} items with `$text` in this channel."
            } else {
              s"OK, I unscheduled `$text` in this channel."
            }
          } else {
            if (listResponses.isEmpty) {
              s"""You don’t currently have anything scheduled in this channel.
                |
                |$viewAllLink
              """.stripMargin
            } else {
              s"I couldn’t find anything scheduled with `$text` in this channel. Here’s what is scheduled currently:\n\n${listResponses.mkString}"
            }
          }
        }
      }).getOrElse(Future.successful("I couldn’t access the scheduling for this channel."))
    } yield {
      SimpleTextResult(event, None, response, forcePrivateResponse = false)
    }
  }

  private def viewAllLink: String = {
    services.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ScheduledActionsController.index(None, None, Some(event.teamId))
      s"[View all scheduled items]($baseUrl$path)"
    }.getOrElse("")
  }
}

object UnscheduleImplementation extends BuiltinImplementationType {

  val builtinId: String = "unschedule"
  val name: String = "Unschedule an action"

  def behaviorVersionsDataFor(team: Team, dataService: DataService) = Seq(
    BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, Some(name), dataService).copy(
      triggers = Seq(BehaviorTriggerData(s"""(?i)^unschedule\\s+([`"'])(.*?)\\1\\s*$$""", requiresMention = true, isRegex = true, caseSensitive = false)),
      builtinName = Some(builtinId)
    )
  )

  def addToGroupDataTo(data: BehaviorGroupData, team: Team, dataService: DataService): BehaviorGroupData = {
    data.copy(
      behaviorVersions = data.behaviorVersions ++ behaviorVersionsDataFor(team, dataService)
    )
  }

}
