package models.behaviors.builtins

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.MessageEvent
import models.behaviors.events.{MessageEvent, SlackMessageAction, SlackMessageActions, SlackMessageEvent}
import models.behaviors.{BotResult, SimpleTextResult, TextWithActionsResult}
import services.{AWSLambdaService, DataService}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         isFirstTrigger: Boolean,
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

  private def maybeHelpSearch: Option[String] = {
    maybeHelpString.filter(_.trim.nonEmpty)
  }

  private def matchString: String = {
    maybeHelpSearch.map { s =>
      s" related to `$s`"
    }.getOrElse("")
  }

  private def introResultFor(groupData: Seq[BehaviorGroupData]): BotResult = {
    val groupsWithOther = ArrayBuffer[BehaviorGroupData]()
    val (untitledGroups, titledGroups) = groupData.partition(group => group.name.isEmpty)
    groupsWithOther ++= titledGroups
    groupsWithOther += BehaviorGroupData(None, "", "", None, untitledGroups.flatMap(group => group.behaviorVersions), None, None, None, OffsetDateTime.now)

    val intro = if (isFirstTrigger) {
      s"What can I help with?"
    } else {
      s"OK, here’s everything else I know$matchString:"
    }
    val instructions = if (matchString.isEmpty) {
      "Click a skill to learn more, or try searching a keyword to narrow it down, e.g. `@ellipsis help bananas`."
    } else {
      "Click a skill to learn more, or try with a different keyword."
    }
    val actions = groupsWithOther.map(group => {
      val (label, helpActionValue) = if (group.name.isEmpty) {
        ("Miscellaneous", "(untitled)")
      } else {
        (group.name, group.id.getOrElse(""))
      }
      SlackMessageAction("help_for_skill", label, helpActionValue)
    })
    val attachment = SlackMessageActions("help", actions, Some(instructions), Some("#F65688"))
    TextWithActionsResult(event, intro, forcePrivateResponse = false, attachment)
  }

  def skillResultFor(group: BehaviorGroupData): BotResult = {

    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString:"
    } else {
      "OK, here’s the help you asked for. Click below for more help."
    }

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
      "_**1 action available:**_  "
    } else {
      s"**$numActions actions available:**  "
    }

    val resultText =
      s"""$name$description
         |
         |$listHeading
         |${group.behaviorVersions.flatMap(helpStringFor).mkString("")}""".stripMargin
    val actions = Seq(SlackMessageAction("help_index", "Other help…", ""))
    TextWithActionsResult(event, intro, forcePrivateResponse = false, SlackMessageActions("help", actions, Some(resultText), Some("#94A4FF")))
  }

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
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
          Seq(BehaviorGroupData(None, "Miscellaneous skills", "", None, groupData.filter(_.name.isEmpty).flatMap(_.behaviorVersions), None, None, None, OffsetDateTime.now))
        } else {
          groupData.filter(_.matchesHelpSearch(helpSearch))
        }
      }.getOrElse {
        groupData
      }
      if (matchingGroupData.length == 1) {
        skillResultFor(matchingGroupData.head)
      } else {
        introResultFor(matchingGroupData)
      }
    }
  }
}
