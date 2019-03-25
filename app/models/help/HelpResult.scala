package models.help

import json.{BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events._
import services.{AWSLambdaService, DataService}

trait HelpResult {
  val event: Event
  val group: HelpGroupData
  val matchingTriggers: Seq[BehaviorTriggerData]
  val botPrefix: String

  val dataService: DataService
  val lambdaService: AWSLambdaService

  def description: String

  private def triggerStringFor(trigger: BehaviorTriggerData): String = {
    val prefix = if (trigger.requiresMention || event.eventContext.shouldForceRequireMention)
      botPrefix
    else
      ""
    val formattedTrigger = s"`$prefix${trigger.text}`"
    if (matchingTriggers.contains(trigger)) {
      s"**$formattedTrigger**"
    } else {
      formattedTrigger
    }
  }

  lazy val triggerableBehaviorVersions: Seq[BehaviorVersionData] = group.behaviorVersions.filter(_.triggers.nonEmpty)
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
