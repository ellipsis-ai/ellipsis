package models.behaviors.builtins

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.Event
import models.behaviors.scheduling.Scheduled
import models.behaviors.{BotResult, SimpleTextResult}
import models.team.Team
import play.api.Configuration
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}


case class ListScheduledBehavior(
                                  event: Event,
                                  services: DefaultServices,
                                  isForAllChannels: Boolean
                                 ) extends BuiltinBehavior {

  val configuration: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val maybeChannel: Option[String] = event.maybeChannel

  private def noMessagesResponse: String = {
    if (isForAllChannels) {
      s"Nothing has been scheduled for this team. $newScheduleLink"
    } else {
      s"""You haven’t yet scheduled anything in this channel. $newScheduleLink
         |
         |$viewAllLink
       """.stripMargin
    }
  }

  private def viewAllLink: String = {
    configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ScheduledActionsController.index(None, None, Some(event.teamId))
      s"[View all scheduled items]($baseUrl$path)"
    }.getOrElse("")
  }

  private def newScheduleLink: String = {
    configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ScheduledActionsController.index(None, Some(true), Some(event.teamId))
      s"[Schedule something new]($baseUrl$path)"
    }.getOrElse("")
  }

  private def intro: String = {
    if (isForAllChannels) {
      "Here’s everything that has been scheduled for this team:"
    } else {
      maybeChannel.map { channel =>
        if (event.isPublicChannel) {
          s"Here’s what you have scheduled in <#$channel>:"
        } else {
          "Here’s what you have scheduled in this channel:"
        }
      }.getOrElse("Here’s what you have scheduled")
    }
  }

  def responseFor(scheduled: Seq[Scheduled])(implicit ec: ExecutionContext): Future[String] = {
    Future.sequence(scheduled.map(ea => ea.listResponse(ea.id, ea.team.id, dataService, configuration, isForAllChannels))).map { listResponses =>
      s"""$intro
        |
        |${listResponses.mkString}
        |
        |$viewAllLink
      """.stripMargin
    }
  }

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      scheduled <- maybeTeam.map { team =>
        maybeChannel.map { channel =>
          Scheduled.allActiveForChannel(team, channel, dataService)
        }.getOrElse {
          Scheduled.allActiveForTeam(team, dataService)
        }
      }.getOrElse(Future.successful(Seq()))
      responseText <- if (scheduled.isEmpty) {
        Future.successful(noMessagesResponse)
      } else {
        responseFor(scheduled)
      }
    } yield {
      SimpleTextResult(event, None, responseText, forcePrivateResponse = false)
    }
  }

}

object ListScheduledBehavior extends BuiltinImplementationType {

  val forAllId: String = "list-all-scheduled"
  val forChannelId: String = "list-channel-scheduled"

  def versionDataFor(builtinId: String, behaviorVersionName: String, trigger: String, team: Team, dataService: DataService): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, Some(behaviorVersionName), dataService).copy(
      triggers = Seq(BehaviorTriggerData(trigger, requiresMention = true, isRegex = true, caseSensitive = false)),
      builtinName = Some(builtinId)
    )
  }

  def behaviorVersionsDataFor(team: Team, dataService: DataService) = Seq(
    versionDataFor(
      forAllId,
      "List all scheduled actions",
      s"""^all scheduled$$""",
      team,
      dataService
    ),
    versionDataFor(
      forChannelId,
      "List scheduled actions for a channel",
      s"""^scheduled$$""",
      team,
      dataService
    )
  )

  def addToGroupDataTo(data: BehaviorGroupData, team: Team, dataService: DataService): BehaviorGroupData = {
    data.copy(
      behaviorVersions = data.behaviorVersions ++ behaviorVersionsDataFor(team, dataService)
    )
  }

}
