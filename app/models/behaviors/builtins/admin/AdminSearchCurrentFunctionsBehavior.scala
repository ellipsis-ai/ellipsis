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
      currentGroupVersionIds <- dataService.behaviorGroupVersions.allCurrentIds
      deployedGroupVersionIds <- Future.traverse(teams) { team =>
        dataService.behaviorGroupDeployments.mostRecentForTeam(team).map(_.map(_.groupVersionId))
      }.map(_.flatten)
      allValidGroupIds <- Future.successful((currentGroupVersionIds ++ deployedGroupVersionIds).distinct)
      matchingBehaviorVersions <- dataService.behaviorVersions.allWithSubstringInGroupVersions(textToMatch, allValidGroupIds)
      matchingLibraryVersions <- dataService.libraries.allWithSubstringInGroupVersions(textToMatch, allValidGroupIds)
    } yield {
      val groupSets = (matchingBehaviorVersions.map(_.groupVersion) ++ matchingLibraryVersions.map(_.groupVersion)).distinct.map { groupVersion =>
        GroupVersionSet(
          groupVersion,
          currentGroupVersionIds.contains(groupVersion.id),
          deployedGroupVersionIds.contains(groupVersion.id),
          matchingBehaviorVersions.filter(_.groupVersion == groupVersion),
          matchingLibraryVersions.filter(_.groupVersion == groupVersion)
        )
      }
      val matchCount = groupSets.foldLeft(0) { (count, teamVersions) =>
        count + teamVersions.behaviorVersions.length + teamVersions.libraryVersions.length
      }
      val groupedByTeam = groupSets.groupBy(_.groupVersion.team)
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
            val currentStatus = if (groupSet.isCurrent) { "current" } else { "_old_" }
            val deployedStatus = if (groupSet.isDeployed) { "deployed" } else { "_not deployed_" }
            val name = s"[${icon}${groupSet.groupVersion.name}](${editSkillLinkFor(groupId)})"
            val title = s"$name — version ID `$groupVersionId` ($currentStatus · $deployedStatus)"
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
