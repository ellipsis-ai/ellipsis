package models.behaviors.builtins.admin

import akka.actor.ActorSystem
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.events.Event
import models.behaviors.library.LibraryVersion
import models.team.Team
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class TeamVersionSet(
                           team: Team,
                           groupVersions: Seq[BehaviorGroupVersion],
                           behaviorVersions: Seq[BehaviorVersion],
                           libraryVersions: Seq[LibraryVersion]
                         )

case class AdminSearchCurrentFunctionsBehavior(searchText: String, event: Event, services: DefaultServices) extends BuiltinAdminBehavior {
  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext ): Future[BotResult] = {
    val textToMatch = searchText.trim.toLowerCase
    for {
      teams <- dataService.teams.allTeams
      allWithBehaviorVersions <- Future.sequence(teams.map { team =>
        dataService.behaviorVersions.allCurrentForTeam(team).map { behaviorVersions =>
          val behaviorVersionMatches = behaviorVersions.filter(_.maybeFunctionBody.exists(_.toLowerCase.contains(textToMatch)))
          TeamVersionSet(team, behaviorVersions.map(_.groupVersion).distinct, behaviorVersionMatches, Seq())
        }
      })
      allWithLibraries <- Future.sequence(allWithBehaviorVersions.flatMap { teamSet =>
        teamSet.groupVersions.map { groupVersion =>
          dataService.libraries.allFor(groupVersion).map { libraryVersions =>
            val libraryMatches = libraryVersions.filter(_.functionBody.toLowerCase.contains(textToMatch))
            teamSet.copy(libraryVersions = libraryMatches)
          }
        }
      })
    } yield {
      val validMatches = allWithLibraries.filter(teamSet => teamSet.behaviorVersions.nonEmpty || teamSet.libraryVersions.nonEmpty)
      val matchCount = validMatches.foldLeft(0) { (count, teamVersions) =>
        count + teamVersions.behaviorVersions.length + teamVersions.libraryVersions.length
      }
      val heading = if (matchCount == 1) {
        s"There is one current function that includes `${searchText}`:"
      } else if (matchCount > 1) {
        s"There are ${matchCount} current functions that include `${searchText}:"
      } else {
        s"There are no current functions that include `${searchText}`."
      }
      val resultText = if (matchCount > 0) {
        heading ++ validMatches.map { teamMatches =>
          val team = teamMatches.team
          s"""---
             |
             |Team: **[${team.name}](${teamLinkFor(team.id)})**
             |
             |Behaviors: ${teamMatches.behaviorVersions.map(version => s"${version.maybeName.getOrElse("(unnamed)")} (ID `${version.behavior.id}`)").mkString(" · ")}
             |
             |Libraries: ${teamMatches.libraryVersions.map(version => s"${version.name} (ID `${version.libraryId}`)").mkString(" · ")}
             |""".stripMargin
        }.mkString("\n")
      } else {
        heading
      }
      SimpleTextResult(event, None, resultText, Normal)
    }
  }
}
