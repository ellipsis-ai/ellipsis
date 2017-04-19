package models.help

import json.{BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.{Event, SlackMessageAction}
import services.{AWSLambdaService, DataService}
import utils.SlackMessageSender

trait HelpResult {
  val event: Event
  val group: HelpGroupData
  val matchingTriggers: Seq[BehaviorTriggerData]

  val dataService: DataService
  val lambdaService: AWSLambdaService

  val slackHelpIndexAction = SlackMessageAction("help_index", "More help…", "0")

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

  case class IndexedBehaviorVersionData(tuple: (BehaviorVersionData, Int)) {
    def version = tuple._1
    def index = tuple._2
    def printableIndex = (index + 1).toString
  }

  def sortedIndexedBehaviorVersions: Seq[IndexedBehaviorVersionData] = {
    val behaviorVersions = group.behaviorVersions
    val trimNonMatching = group.isMiscellaneous
    val (matching, nonMatching) = behaviorVersions.filter(_.triggers.nonEmpty).partition(version => version.triggers.exists(matchingTriggers.contains))
    val list = if (trimNonMatching && matching.nonEmpty) {
      matching
    } else {
      matching ++ nonMatching
    }
    list.zipWithIndex.map(IndexedBehaviorVersionData)
  }

  def slackRunActionsFor(indexedVersions: Seq[IndexedBehaviorVersionData]): Seq[SlackMessageAction] = {
    val maxRunnableActions = SlackMessageSender.MAX_ACTIONS_PER_ATTACHMENT - 1
    val actions = indexedVersions.slice(0, maxRunnableActions)
    val includeIndexes = actions.length > 1
    actions.flatMap { ea =>
      val label = if (includeIndexes) { ea.printableIndex } else { "Run this" }
      ea.version.id.map { versionId =>
        SlackMessageAction("run_action", label, versionId)
      }
    }
  }

  def helpTextFor(indexedVersions: Seq[IndexedBehaviorVersionData]): String = {
    val includeIndexes = indexedVersions.length > 1
    indexedVersions.flatMap { ea => maybeHelpStringFor(ea.version, Option(ea.printableIndex).filter(_ => includeIndexes)) }.mkString("")
  }

  def maybeHelpStringFor(behaviorVersionData: BehaviorVersionData, maybePrintableIndex: Option[String]): Option[String] = {
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
        val index = maybePrintableIndex.map(_.mkString("", "", ". ")).getOrElse("")
        val linkText = (for {
          groupId <- behaviorVersionData.groupId
          behaviorId <- behaviorVersionData.behaviorId
        } yield {
          val url = dataService.behaviors.editLinkFor(groupId, Some(behaviorId), lambdaService.configuration)
          s" [✎]($url)"
        }).getOrElse("")
        Some(s"$index$triggersString$linkText\n\n")
      }
    }
  }
}
