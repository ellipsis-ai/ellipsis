package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.scheduling.Scheduled
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.Configuration
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ListScheduledBehavior(
                                  event: Event,
                                  maybeChannel: Option[String],
                                  lambdaService: AWSLambdaService,
                                  dataService: DataService,
                                  configuration: Configuration
                                 ) extends BuiltinBehavior {

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
    configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ScheduledActionsController.index(None, None, Some(event.teamId))
      s"[View all scheduled items]($baseUrl$path)"
    }.getOrElse("")
  }

  private def newScheduleLink: String = {
    configuration.getString("application.apiBaseUrl").map { baseUrl =>
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

  def responseFor(scheduled: Seq[Scheduled]): Future[String] = {
    Future.sequence(scheduled.map(ea => ea.listResponse(ea.id, ea.team.id, dataService, configuration, maybeChannel.isEmpty))).map { listResponses =>
      s"""$intro
        |
        |${listResponses.mkString}
        |
        |$viewAllLink
      """.stripMargin
    }
  }

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
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
