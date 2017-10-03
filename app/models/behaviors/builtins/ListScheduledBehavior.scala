package models.behaviors.builtins

import akka.actor.ActorSystem
import json.{BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.scheduling.Scheduled
import models.behaviors.{BotResult, ParameterWithValue, SimpleTextResult}
import models.team.Team
import play.api.Configuration
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}


case class ListScheduledBehavior(
                                  event: Event,
                                  services: DefaultServices
                                 ) extends BuiltinBehavior {

  val configuration: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val maybeChannel: Option[String] = event.maybeChannel

  private def noMessagesResponse: String = {
    if (maybeChannel.isDefined) {
      s"""You haven’t yet scheduled anything in this channel. $newScheduleLink
         |
         |$viewAllLink
       """.stripMargin
    } else {
      s"Nothing has been scheduled for this team. $newScheduleLink"
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
    maybeChannel.map { channel =>
      if (event.isPublicChannel) {
        s"Here’s what you have scheduled in <#$channel>:"
      } else {
        "Here’s what you have scheduled in this channel:"
      }
    }.getOrElse {
      "Here’s everything that has been scheduled for this team:"
    }
  }

  def responseFor(scheduled: Seq[Scheduled])(implicit ec: ExecutionContext): Future[String] = {
    Future.sequence(scheduled.map(ea => ea.listResponse(ea.id, ea.team.id, dataService, configuration, maybeChannel.isEmpty))).map { listResponses =>
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

  def resultFor(
                 parametersWithValues: Seq[ParameterWithValue],
                 maybeConversation: Option[Conversation]
               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      maybeTeam <- services.dataService.teams.find(event.teamId)
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
      SimpleTextResult(event, maybeConversation, responseText, forcePrivateResponse = false)
    }
  }

}

object ListScheduledBehavior {

  val name: String = "list-all-scheduled"

  def newVersionDataFor(team: Team, dataService: DataService): BehaviorVersionData = {
    BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, Some("List all scheduled actions"), dataService).copy(
      triggers = Seq(BehaviorTriggerData(s"""^all scheduled$$""", requiresMention = true, isRegex = true, caseSensitive = false)),
      builtinName = Some(name)
    )
  }

}
