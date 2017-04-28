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
    val formattedTrigger = s"`$prefix${trigger.text}`"
    if (matchingTriggers.contains(trigger)) {
      s"**$formattedTrigger**"
    } else {
      formattedTrigger
    }
  }

  private lazy val triggerableBehaviorVersions: Seq[BehaviorVersionData] = group.behaviorVersions.filter(_.triggers.nonEmpty)
  lazy val matchingBehaviorVersions: Seq[BehaviorVersionData] = triggerableBehaviorVersions.filter(_.triggers.exists(matchingTriggers.contains))
  lazy val nonMatchingBehaviorVersions: Seq[BehaviorVersionData] = triggerableBehaviorVersions.filterNot(_.triggers.exists(matchingTriggers.contains))

  def behaviorVersionsToDisplay(includeNonMatching: Boolean = false): Seq[BehaviorVersionData] = {
    if (matchingBehaviorVersions.nonEmpty) {
      if (includeNonMatching) {
        matchingBehaviorVersions ++ nonMatchingBehaviorVersions
      } else {
        matchingBehaviorVersions
      }
    } else {
      nonMatchingBehaviorVersions
    }
  }

  def maybeShowAllBehaviorVersionsAction(maybeSearchText: Option[String], includeNonMatchingResults: Boolean = false): Option[SlackMessageAction] = {
    if (!includeNonMatchingResults && !group.isMiscellaneous && matchingBehaviorVersions.nonEmpty && nonMatchingBehaviorVersions.nonEmpty) {
      val numVersions = triggerableBehaviorVersions.length
      val label = if (numVersions == 2) {
        s"Show both actions"
      } else {
        s"Show all $numVersions actions"
      }
      Some(SlackMessageActionButton(LIST_BEHAVIOR_GROUP_ACTIONS, label, HelpGroupSearchValue(group.helpActionId, maybeSearchText).toString))
    } else {
      None
    }
  }

  def slackRunActionsFor(behaviorVersions: Seq[BehaviorVersionData]): Seq[SlackMessageAction] = {
    if (behaviorVersions.length == 1) {
      behaviorVersions.headOption.flatMap { version =>
        version.id.map { versionId =>
          Seq(SlackMessageActionButton(RUN_BEHAVIOR_VERSION, "Run this action", versionId))
        }
      }.getOrElse(Seq())
    } else {
      val menuItems = behaviorVersions.flatMap { ea =>
        ea.id.map { behaviorVersionId =>
          SlackMessageActionMenuItem(ea.maybeFirstTrigger.getOrElse("Run"), behaviorVersionId)
        }
      }
      Seq(SlackMessageActionMenu(RUN_BEHAVIOR_VERSION, "Actions", menuItems))
    }
  }

  def helpTextFor(behaviorVersions: Seq[BehaviorVersionData]): String = {
    behaviorVersions.flatMap(ea => maybeHelpStringFor(ea)).mkString("\n\n")
  }

  def behaviorVersionsHeading(includeNonMatching: Boolean): String = {
    if (!includeNonMatching && matchingBehaviorVersions.nonEmpty) {
      val numActions = matchingBehaviorVersions.length
      if (numActions == 1) {
        "_**1 matching action**_"
      } else {
        s"_**$numActions matching actions**_"
      }
    } else {
      val numActions = triggerableBehaviorVersions.length
      if (numActions == 0) {
        "No actions to display."
      } else if (numActions == 1) {
        "_**1 action**_"
      } else {
        s"_**$numActions actions**_"
      }
    }
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
        Some(s"$triggersString$linkText")
      }
    }
  }
}
