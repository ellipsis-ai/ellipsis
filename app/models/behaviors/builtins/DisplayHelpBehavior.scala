package models.behaviors.builtins

import java.time.OffsetDateTime

import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.{MessageEvent, SlackMessageAction, SlackMessageActions, SlackMessageEvent}
import models.behaviors.{BotResult, SimpleTextResult, TextWithActionsResult}
import services.{AWSLambdaService, DataService}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         event: MessageEvent,
                         lambdaService: AWSLambdaService,
                         dataService: DataService
                       ) extends BuiltinBehavior {

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
        Some(s"$triggersString$link $authorsString  \n")
      }
    }
  }

  private def triggerStringFor(trigger: BehaviorTriggerData): String = {
    if (trigger.requiresMention)
      s"`${event.botPrefix}${trigger.text}`"
    else
      s"`${trigger.text}`"
  }

  private def introResultFor(groupData: Seq[BehaviorGroupData], maybeHelpSearch: Option[String]): BotResult = {
    val groupsWithOther = ArrayBuffer[BehaviorGroupData]()
    val (untitledGroups, titledGroups) = groupData.partition(group => group.name.isEmpty)
    groupsWithOther ++= titledGroups
    groupsWithOther += BehaviorGroupData(None, "", "", None, untitledGroups.flatMap(group => group.behaviorVersions), None, None, None, OffsetDateTime.now)
    val matchString = maybeHelpSearch.map { s =>
      s" related to `$s`"
    }.getOrElse("")
    val tryAgain = if (matchString.isEmpty) {
      "with a keyword to narrow it down, e.g. `@ellipsis help bananas`."
    } else {
      "with a different keyword."
    }
    val intro =
      s"""Here’s what I can do$matchString.
         |
         |Click the skill you want to know more about, or try again $tryAgain""".stripMargin
    val actions = groupsWithOther.map(group => {
      val (label, helpActionValue) = if (group.name.isEmpty) {
        ("Other", "(untitled)")
      } else {
        (group.name, group.id.getOrElse(""))
      }
      SlackMessageAction("help_for_skill", label, helpActionValue)
    })
    TextWithActionsResult(event, intro, forcePrivateResponse = false, SlackMessageActions("help", actions, None))
  }

  def skillResultFor(group: BehaviorGroupData): BotResult = {
    val name = if (group.name.isEmpty) {
      "**Miscellaneous skills**"
    } else {
      s"**${group.name}**"
    }

    val description = if (group.description.isEmpty) {
      ""
    } else {
      s"  \n${group.description}"
    }

    val numActions = group.behaviorVersions.length

    val listHeading = if (numActions == 0) {
      "This skill has no actions."
    } else if (numActions == 1) {
      "**1 action available:**  "
    } else {
      s"**$numActions actions available:**  "
    }

    val resultText =
      s"""$name$description
         |
         |$listHeading
         |${group.behaviorVersions.flatMap(helpStringFor).mkString("")}""".stripMargin
    SimpleTextResult(event, resultText, forcePrivateResponse = false)
  }

  def result: Future[BotResult] = {
    val maybeHelpSearch = maybeHelpString.filter(_.trim.nonEmpty)
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      user <- event.ensureUser(dataService)
      maybeBehaviorGroups <- maybeTeam.map { team =>
        maybeSkillId.map(skillId => {
          dataService.behaviorGroups.find(skillId).map(_.map(Seq(_)))
        }).getOrElse({
          dataService.behaviorGroups.allFor(team).map(Some(_))
        })
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
        if (helpSearch == "(untitled)") {
          Seq(BehaviorGroupData(None, "Other", "", None, groupData.filter(_.name.isEmpty).flatMap(_.behaviorVersions), None, None, None, OffsetDateTime.now))
        } else {
          groupData.filter(_.matchesHelpSearch(helpSearch))
        }
      }.getOrElse {
        groupData
      }
      if (matchingGroupData.length == 1) {
        skillResultFor(matchingGroupData.head)
      } else {
        introResultFor(matchingGroupData, maybeHelpSearch)
      }
    }
  }
}
