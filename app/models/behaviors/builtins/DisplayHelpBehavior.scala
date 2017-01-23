package models.behaviors.builtins

import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.{MessageEvent, SlackMessageEvent}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                                helpString: String,
                                event: MessageEvent,
                                lambdaService: AWSLambdaService,
                                dataService: DataService
                              ) extends BuiltinBehavior {

  private def triggerStringFor(trigger: BehaviorTriggerData): String = {
    if (trigger.requiresMention)
      s"`...${trigger.text}`"
    else
      s"`${trigger.text}`"
  }

  private def helpStringFor(behaviorVersion: BehaviorVersionData): Option[String] = {
    val triggers = behaviorVersion.triggers
    if (triggers.isEmpty) {
      None
    } else {
      val nonRegexTriggers = triggers.filterNot(_.isRegex)
      val namedTriggers =
        if (nonRegexTriggers.isEmpty)
          triggerStringFor(triggers.head)
        else
          nonRegexTriggers.map(triggerStringFor).mkString(" ")
      val regexTriggerCount =
        if (nonRegexTriggers.isEmpty)
          triggers.tail.count({ ea => ea.isRegex })
        else
          triggers.count({ ea => ea.isRegex })

      val regexTriggerString =
        if (regexTriggerCount == 1)
          s" _(also matches another pattern)_"
        else if (regexTriggerCount > 1)
          s" _(also matches $regexTriggerCount other patterns)_"
        else
          s""

      val triggersString = namedTriggers ++ regexTriggerString
      if (triggersString.isEmpty) {
        None
      } else {
        val maybeLink = behaviorVersion.behaviorId.map { id =>
          dataService.behaviors.editLinkFor(id, lambdaService.configuration)
        }
        val link = maybeLink.map { l => s" [✎]($l)" }.getOrElse("")
        val authorsString = "" //if (authorNames.isEmpty) { "" } else { "by " ++ authorNames.map(n => s"<@$n>").mkString(", ") }
        Some(s"\n$triggersString$link $authorsString  ")
      }
    }
  }

  private def helpStringFor(group: BehaviorGroupData): String = {
    s"""\n**${group.name}**
       |${group.behaviorVersions.map(helpStringFor).flatten.mkString("")}
     """.stripMargin
  }

  private def helpStringFor(groups: Seq[BehaviorGroupData], prompt: String, matchString: String): String = {
    val (groupsWithNames, groupsWithoutNames) = groups.partition(_.maybeNonEmptyName.isDefined)
    val groupsWithNamesString = groupsWithNames.map { ea =>
      helpStringFor(ea)
    }.mkString("")
    val groupsWithoutNamesString = groupsWithoutNames.map { ea =>
      helpStringFor(ea)
    }.mkString("")
    val hasGroupsWithNames = groupsWithNamesString.trim.nonEmpty
    val hasGroupsWithoutNames = groupsWithoutNamesString.trim.nonEmpty
    val unnamedSkillsString = if(hasGroupsWithoutNames) { "\n**Unnamed skills:**  \n" } else { "" }
    if (!hasGroupsWithNames && !hasGroupsWithoutNames) {
      ""
    } else {
      s"""$prompt$matchString:
         |$groupsWithNamesString$unnamedSkillsString$groupsWithoutNamesString
         |""".stripMargin
    }
  }

  def justGettingStartedText(lambdaService: AWSLambdaService): String = {
    s"""I’m just getting started here and can’t wait to learn.
      |
      |You can ${event.installLinkFor(lambdaService)} or ${event.teachMeLinkFor(lambdaService)} yourself.""".stripMargin
  }

  def result: Future[BotResult] = {
    val maybeHelpSearch = Option(helpString).filter(_.trim.nonEmpty)
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      user <- event.ensureUser(dataService)
      maybeBehaviorGroups <- maybeTeam.map { team =>
        dataService.behaviorGroups.allFor(team).map(Some(_))
      }.getOrElse {
        Future.successful(None)
      }
      groupData <- maybeBehaviorGroups.map { groups =>
        Future.sequence(groups.map { group =>
          BehaviorGroupData.maybeFor(group.id, user, None, dataService)
        }).map(_.flatten.sorted)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val matchingGroupData = maybeHelpSearch.map { helpSearch =>
        groupData.filter(_.matchesHelpSearch(helpSearch))
      }.getOrElse {
        groupData
      }
      val matchString = maybeHelpSearch.map { s =>
        s" related to `$s`"
      }.getOrElse("")
      val groupsString = helpStringFor(matchingGroupData, "Here’s what I can do", matchString)
      val endingString = if (groupData.isEmpty) {
        justGettingStartedText(lambdaService)
      } else if (matchingGroupData.isEmpty) {
        maybeHelpSearch.map { s =>
          s"""I couldn’t find help for anything like `$s`
           |
           |Try searching for something else, e.g. `${event.botPrefix}help bananas`
           |
           |Or type `${event.botPrefix}help` to list everything I can do.
         """.stripMargin
        }.getOrElse {
          justGettingStartedText(lambdaService)
        }
      } else {
        s"You can also ${event.installLinkFor(lambdaService)} or ${event.teachMeLinkFor(lambdaService)} yourself."
      }
      val text = s"""
          |$groupsString
          |
          |$endingString
          |""".stripMargin
      SimpleTextResult(text, forcePrivateResponse = false)
    }
  }

}
