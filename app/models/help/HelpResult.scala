package models.help

import json.{BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.Event
import services.{AWSLambdaService, DataService}

trait HelpResult {
  val event: Event
  val group: HelpGroupData
  val matchingTriggers: Seq[BehaviorTriggerData]

  val dataService: DataService
  val lambdaService: AWSLambdaService

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

  def sortedActionListFor(behaviorVersions: Seq[BehaviorVersionData], trimNonMatching: Boolean = false): Seq[String] = {
    val (matching, nonMatching) = behaviorVersions.partition(version => version.triggers.exists(matchingTriggers.contains))
    val versionsToInclude = if (trimNonMatching && matching.nonEmpty) { matching } else { matching ++ nonMatching }
    versionsToInclude.flatMap(version => helpStringFor(version))
  }

  def helpStringFor(behaviorVersion: BehaviorVersionData): Option[String] = {
    val triggers = behaviorVersion.triggers
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
          groupId <- behaviorVersion.groupId
          behaviorId <- behaviorVersion.behaviorId
        } yield {
          val url = dataService.behaviors.editLinkFor(groupId, Some(behaviorId), lambdaService.configuration)
          s" [✎]($url)"
        }).getOrElse("")
        Some(s"$triggersString$linkText\n\n")
      }
    }
  }
}
