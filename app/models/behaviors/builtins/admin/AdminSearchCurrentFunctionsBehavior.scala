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
  val MAX_EDITABLES_TO_DISPLAY = 3
  val MAX_GROUP_VERSIONS_TO_DISPLAY = 10

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext ): Future[BotResult] = {
    val textToMatch = searchText.trim
    for {
      currentGroupVersionIds <- dataService.behaviorGroupVersions.allCurrentIds
      deployedGroupVersionIds <- dataService.behaviorGroupDeployments.mostRecentBehaviorGroupVersionIds
      allValidGroupIds <- Future.successful((currentGroupVersionIds ++ deployedGroupVersionIds).distinct)
      matchingBehaviorVersions <- dataService.behaviorVersions.allWithSubstringInGroupVersions(textToMatch, allValidGroupIds)
      matchingLibraryVersions <- dataService.libraries.allWithSubstringInGroupVersions(textToMatch, allValidGroupIds)
    } yield {
      val matchCount = matchingBehaviorVersions.length + matchingLibraryVersions.length
      val groupVersions = (matchingBehaviorVersions.map(_.groupVersion) ++ matchingLibraryVersions.map(_.groupVersion)).distinct.slice(0, MAX_GROUP_VERSIONS_TO_DISPLAY)
      val allGroupSets = groupVersions.map { groupVersion =>
        GroupVersionSet(
          groupVersion,
          currentGroupVersionIds.contains(groupVersion.id),
          deployedGroupVersionIds.contains(groupVersion.id),
          matchingBehaviorVersions.filter(_.groupVersion == groupVersion),
          matchingLibraryVersions.filter(_.groupVersion == groupVersion)
        )
      }
      val displayedMatchCount = allGroupSets.foldLeft(0) { (count, teamVersions) =>
        count + teamVersions.behaviorVersions.length + teamVersions.libraryVersions.length
      }
      val teamCount = allGroupSets.map(_.groupVersion.team).distinct.length
      val groupedByTeam = allGroupSets.groupBy(_.groupVersion.team)
      val teamInfo = if (teamCount == 1) {
        "all on 1 team"
      } else {
        s"across ${teamCount} teams"
      }
      val truncatedInfo = if (matchCount > displayedMatchCount) {
        s" Showing the first ${displayedMatchCount} matches:"
      } else {
        ""
      }
      val heading = (if (matchCount == 1) {
        s"There is one current/deployed function that includes `${searchText}`."
      } else if (matchCount > 1) {
        s"There are ${matchCount} current/deployed functions $teamInfo that include `${searchText}`."
      } else {
        s"There are no current or deployed functions $teamInfo that include `${searchText}`."
      }) ++ truncatedInfo
      val resultText = heading ++ groupedByTeam.map(formatTeamGroupSets).mkString("")
      SimpleTextResult(event, None, resultText, Normal)
    }
  }

  case class EditableInfo(id: String, name: String)

  def formatBehaviorVersions(prefix: String, versions: Seq[BehaviorVersion], groupId: String): String = {
    formatEditable(prefix, versions.map(ea => EditableInfo(ea.behavior.id, ea.exportName)), groupId)
  }

  def formatLibraries(versions: Seq[LibraryVersion], groupId: String): String = {
    formatEditable("🧰", versions.map(ea => EditableInfo(ea.libraryId, ea.name)), groupId)
  }

  def formatEditable(prefix: String, versions: Seq[EditableInfo], groupId: String): String = {
    if (versions.nonEmpty) {
      val truncated = versions.slice(0, MAX_EDITABLES_TO_DISPLAY)
      val remainder = versions.length - truncated.length
      val truncatedInfo = if (remainder == 1) {
        " _and 1 other_"
      } else if (remainder > 1) {
        s" _and ${remainder} others_"
      } else {
        ""
      }
      s"> $prefix ${truncated.map { version =>
        val link = editSkillLinkFor(groupId, Some(version.id))
        s"[${version.name}]($link)"
      }.mkString(" · ")}${truncatedInfo}\n"
    } else {
      ""
    }
  }

  def formatTeamGroupSets(teamGroupSet: (Team, Seq[GroupVersionSet])): String = {
    teamGroupSet match {
      case (team, teamGroupSets) => {
        val teamInfo = s"Team: **[${team.name}](${teamLinkFor(team.id)})**"

        val groups = teamGroupSets.sortBy(_.groupVersion.name.toLowerCase).map(formatGroupVersionSet)
        s"""
           |
           |---
           |
           |${teamInfo}
           |
           |${groups.mkString("\n\n·\n\n")}
           |
           |""".stripMargin
      }
    }
  }

  def formatGroupVersionSet(groupSet: GroupVersionSet): String = {
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
    val actionInfo = formatBehaviorVersions("🎬", actions, groupId)
    val dataTypeInfo = formatBehaviorVersions("📁", dataTypes, groupId)
    val testInfo = formatBehaviorVersions("📐", tests, groupId)
    val libraryInfo = formatLibraries(groupSet.libraryVersions, groupId)
    s"""$title
       |
       |$actionInfo$dataTypeInfo$testInfo$libraryInfo
       |
       |""".stripMargin
  }
}
