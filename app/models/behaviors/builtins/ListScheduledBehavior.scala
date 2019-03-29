package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.Normal
import models.behaviors.events.Event
import models.behaviors.scheduling.Scheduled
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.Configuration
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}


case class ListScheduledBehavior(
                                  event: Event,
                                  maybeChannel: Option[String],
                                  services: DefaultServices
                                 ) extends BuiltinBehavior {

  val configuration: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val baseUrl: String = configuration.get[String]("application.apiBaseUrl")

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
    val path = controllers.routes.ScheduledActionsController.index(
      selectedId = None,
      newSchedule = None,
      channelId = None,
      teamId = Some(event.ellipsisTeamId),
      forceAdmin = None
    )
    s"[View all scheduled items]($baseUrl$path)"
  }

  private def newScheduleLink: String = {
    val path = controllers.routes.ScheduledActionsController.index(
      selectedId = None,
      newSchedule = Some(true),
      channelId = None,
      teamId = Some(event.ellipsisTeamId),
      forceAdmin = None
    )
    s"[Schedule something new]($baseUrl$path)"
  }

  private def viewChannelURL(channelId: String): String = {
    val path = controllers.routes.ScheduledActionsController.index(
      selectedId = None,
      newSchedule = None,
      channelId = Some(channelId),
      teamId = Some(event.ellipsisTeamId),
      forceAdmin = None
    )
    baseUrl + path.url
  }

  private def intro: String = {
    maybeChannel.map { channel =>
      val url = viewChannelURL(channel)
      if (event.isPublicChannel) {
        s"Here’s [what you have scheduled]($url) in <#$channel>:"
      } else {
        s"Here’s what you have [scheduled in this channel]($url):"
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
      maybeTeam <- dataService.teams.find(event.ellipsisTeamId)
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
      SimpleTextResult(event, None, responseText, responseType = Normal)
    }
  }

}
