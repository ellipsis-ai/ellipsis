package models.help

import json.{BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import services.{AWSLambdaService, DataService}

trait HelpResult {
  val event: Event
  val group: HelpGroupData
  val matchingTriggers: Seq[BehaviorTriggerData]

  val dataService: DataService
  val lambdaService: AWSLambdaService

  val slackHelpIndexAction = SlackMessageActionButton(SHOW_HELP_INDEX, "More help…", "0")

  def description: String

  private def triggerStringFor(trigger: BehaviorTriggerData): String = {
    val prefix = if (trigger.requiresMention)
      event.botPrefix
    else
      ""
    if (matchingTriggers.contains(trigger)) {
      s"**`$prefix${trigger.text}`**"
    } else {
      s"`$prefix${trigger.text}`"
    }
  }

  def sortedBehaviorVersions: Seq[BehaviorVersionData] = {
    val behaviorVersions = group.behaviorVersions
    val trimNonMatching = group.isMiscellaneous
    val (matching, nonMatching) = behaviorVersions.filter(_.triggers.nonEmpty).partition(version => version.triggers.exists(matchingTriggers.contains))
    if (trimNonMatching && matching.nonEmpty) {
      matching
    } else {
      matching ++ nonMatching
    }
  }

  def slackRunActionsFor(behaviorVersions: Seq[BehaviorVersionData]): Seq[SlackMessageAction] = {
    if (behaviorVersions.length == 1) {
      behaviorVersions.headOption.flatMap { version =>
        version.id.map { versionId =>
          Seq(SlackMessageActionButton("run_behavior_version", "Run this action", versionId))
        }
      }.getOrElse(Seq())
    } else {
      val menuItems = behaviorVersions.flatMap { ea =>
        ea.id.map { behaviorVersionId =>
          SlackMessageActionMenuItem(ea.maybeFirstTrigger.getOrElse("Run"), behaviorVersionId)
        }
      }
      Seq(SlackMessageActionMenu("run_behavior_version", "Actions", menuItems))
    }
  }

  def helpTextFor(behaviorVersions: Seq[BehaviorVersionData]): String = {
    behaviorVersions.flatMap { ea => maybeHelpStringFor(ea) }.mkString("")
  }

  def maybeHelpStringFor(behaviorVersionData: BehaviorVersionData): Option[String] = {
    val triggers = behaviorVersionData.triggers
    if (triggers.isEmpty) {
      None
    } else {
      val nonRegexTriggers = triggers.filterNot(_.isRegex)
      val namedTriggers =
        if (nonRegexTriggers.isEmpty)
          triggerStringFor(triggers.head)
        else
          nonRegexTriggers.map(trigger => triggerStringFor(trigger)).mkString(" ")
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
        val linkText = (for {
          groupId <- behaviorVersionData.groupId
          behaviorId <- behaviorVersionData.behaviorId
        } yield {
          val url = dataService.behaviors.editLinkFor(groupId, Some(behaviorId), lambdaService.configuration)
          s" [✎]($url)"
        }).getOrElse("")
        Some(s"$triggersString$linkText\n\n")
      }
    }
  }
}
