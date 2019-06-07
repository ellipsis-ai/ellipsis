package models.behaviors.builtins.admin

import java.time.format.{DateTimeFormatter, TextStyle}
import java.util.Locale

import akka.actor.ActorSystem
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.behaviorversion.Normal
import models.behaviors.events.Event
import models.team.Team
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class AdminLookupSlackTeamBehavior(slackTeamId: String, event: Event, services: DefaultServices) extends BuiltinAdminBehavior {

  def result(
              implicit actorSystem: ActorSystem,
              ec: ExecutionContext
            ): Future[BotResult] = {
    for {
      slackBotProfiles <- dataService.slackBotProfiles.allForSlackTeamId(slackTeamId)
      teams <- Future.sequence(slackBotProfiles.map(_.teamId).distinct.map { teamId =>
        dataService.teams.find(teamId)
      }).map(_.flatten)
    } yield {
      val message = if (teams.isEmpty) {
        s"No Ellipsis teams found for Slack team ID `${slackTeamId}`"
      } else {
        val teamText = teams.map(teamTextFor).mkString("\n\n---\n\n")
        val intro = if (teams.length == 1) {
          "I found the team:"
        } else {
          s"I found ${teams.length} teams:"
        }
        s"$intro\n\n$teamText\n"
      }
      SimpleTextResult(event, None, message, Normal)
    }
  }

  private def teamTextFor(team: Team): String = {
    s"""Ellipsis team **[${team.name}](${teamLinkFor(team.id)})** (ID #${team.id}) created on ${team.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}
       |Time zone: ${team.timeZone.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}
       |""".stripMargin

  }
}
