package models.behaviors.builtins.admin

import akka.actor.ActorSystem
import json.LibraryVersionData
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.events.Event
import models.behaviors.library.LibraryVersion
import models.team.Team
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class GroupVersionSet(
                            groupVersion: BehaviorGroupVersion,
                            isCurrent: Boolean,
                            isDeployed: Boolean,
                            behaviorVersions: Seq[BehaviorVersion] = Seq(),
                            libraryVersions: Seq[LibraryVersion] = Seq()
                          )

case class AdminSearchCurrentFunctionsBehavior(searchText: String, event: Event, services: DefaultServices) extends BuiltinAdminBehavior {
  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext ): Future[BotResult] = {
    val textToMatch = searchText.trim
    for {
      teams <- dataService.teams.allTeams
      allBehaviorGroups <- Future.traverse(teams) { team =>
        dataService.behaviorGroups.allFor(team)
      }.map(_.flatten)
      allTeamsDeployments <- Future.traverse(teams) { team =>
        dataService.behaviorGroupDeployments.mostRecentForTeam(team)
      }.map(_.flatten)
      currentGroupVersionSets <- Future.traverse(allBehaviorGroups) { group =>
        dataService.behaviorGroupVersions.maybeCurrentFor(group)
      }.map(_.flatten.map { version =>
        GroupVersionSet(version, isCurrent = true, isDeployed = allTeamsDeployments.exists(_.groupVersionId == version.id))
      })
      oldDeployedGroupVersionSets <- {
        val nonCurrentDeployedIds = allTeamsDeployments.map(_.groupVersionId).distinct.filterNot { deployedId =>
          currentGroupVersionSets.exists(current => current.groupVersion.id == deployedId)
        }
        Future.traverse(nonCurrentDeployedIds) { groupVersionId =>
          dataService.behaviorGroupVersions.findWithoutAccessCheck(groupVersionId)
        }.map(_.flatten.map(GroupVersionSet(_, isCurrent = false, isDeployed = true)))
      }
      withBehaviorVersionMatches <- {
        val allSets = currentGroupVersionSets ++ oldDeployedGroupVersionSets
        Future.traverse(allSets) { set =>
          dataService.behaviorVersions.allForGroupVersion(set.groupVersion).map { behaviorVersions =>
            val behaviorVersionMatches = behaviorVersions.filter(_.maybeFunctionBody.exists(_.contains(textToMatch)))
            set.copy(behaviorVersions = behaviorVersionMatches)
          }
        }
      }
      allGroupSets <- Future.traverse(withBehaviorVersionMatches) { groupSet =>
        dataService.libraries.allFor(groupSet.groupVersion).map { libraryVersions =>
          val libraryMatches = libraryVersions.filter(_.functionBody.contains(textToMatch))
          groupSet.copy(libraryVersions = libraryMatches)
        }
      }
    } yield {
      val validGroupSets = allGroupSets.filter(groupSet => groupSet.behaviorVersions.nonEmpty || groupSet.libraryVersions.nonEmpty)
      val matchCount = validGroupSets.foldLeft(0) { (count, teamVersions) =>
        count + teamVersions.behaviorVersions.length + teamVersions.libraryVersions.length
      }
      val groupedByTeam = validGroupSets.groupBy(_.groupVersion.team)
      val heading = if (matchCount == 1) {
        s"There is one current/deployed function that includes `${searchText}`:"
      } else if (matchCount > 1) {
        s"There are ${matchCount} current/deployed functions that include `${searchText}`:"
      } else {
        s"There are no current or deployed functions that include `${searchText}`."
      }
      val resultText = if (matchCount > 0) {
        val teamResults = groupedByTeam.map { case(team, groupSets) =>
          val teamInfo = s"Team: **[${team.name}](${teamLinkFor(team.id)})**"

          val groups = groupSets.map { groupSet =>
            val groupVersionId = groupSet.groupVersion.id
            val groupId = groupSet.groupVersion.group.id
            val icon = groupSet.groupVersion.maybeIcon.map(_ + " ").getOrElse("")
            val currentStatus = if (groupSet.isCurrent) { "(current)" } else { "(non-current)"}
            val deployedStatus = if (groupSet.isDeployed) { "(deployed)" } else { "(not deployed)" }
            val name = s"[${icon}${groupSet.groupVersion.name}](${editSkillLinkFor(groupId)})"
            val title = s"$name (version ID `$groupVersionId`) $currentStatus $deployedStatus"
            val dataTypes = groupSet.behaviorVersions.filter(_.isDataType)
            val tests = groupSet.behaviorVersions.filter(_.isTest)
            val actions = groupSet.behaviorVersions.filterNot(ea => ea.isDataType || ea.isTest)
            val actionInfo = formatBehaviorVersions("\uD83C\uDFAC", actions, groupId)
            val dataTypeInfo = formatBehaviorVersions("\uD83D\uDCC1", dataTypes, groupId)
            val testInfo = formatBehaviorVersions("\uD83D\uDCD0", tests, groupId)
            val libraryInfo = formatLibraries(groupSet.libraryVersions, groupId)
            s"""$title
               |
               |$actionInfo$dataTypeInfo$testInfo$libraryInfo
               |
               |""".stripMargin
          }
          s"""
             |
             |---
             |
             |${teamInfo}
             |
             |${groups.mkString("\n\n·\n\n")}
             |
             |""".stripMargin
        }.mkString("")
        heading ++ teamResults
      } else {
        heading
      }
      SimpleTextResult(event, None, resultText, Normal)
    }
  }

  def formatBehaviorVersions(prefix: String, versions: Seq[BehaviorVersion], groupId: String): String = {
    if (versions.nonEmpty) {
      s"> $prefix ${versions.map { version =>
        val link = editSkillLinkFor(groupId, Some(version.behavior.id))
        s"[${version.exportName}]($link)"
      }.mkString(" · ")}\n"
    } else {
      ""
    }
  }

  def formatLibraries(versions: Seq[LibraryVersion], groupId: String): String = {
    if (versions.nonEmpty) {
      s"> 🧰 ${versions.map { version =>
        val link = editSkillLinkFor(groupId, Some(version.libraryId))
        s"[${version.name}]($link)"
      }.mkString(" · ")}\n"
    } else {
      ""
    }
  }
}
