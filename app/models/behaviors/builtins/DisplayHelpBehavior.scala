package models.behaviors.builtins

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events._
import utils._
import models.behaviors.{BotResult, TextWithActionsResult}
import services.{AWSLambdaService, DataService}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         maybeStartAtIndex: Option[Int],
                         isFirstTrigger: Boolean,
                         event: Event,
                         lambdaService: AWSLambdaService,
                         dataService: DataService
                       ) extends BuiltinBehavior {

  private def searchPatternFor(searchText: String): Regex = {
    s"(?i)(\\s|\\A)(\\S*${Regex.quote(searchText)}\\S*)(\\s|\\Z)".r
  }

  private def helpStringFor(result: HelpResult, behaviorVersion: BehaviorVersionData): Option[String] = {
    val triggers = behaviorVersion.triggers
    if (triggers.isEmpty) {
      None
    } else {
      val nonRegexTriggers = triggers.filterNot(_.isRegex)
      val namedTriggers =
        if (nonRegexTriggers.isEmpty)
          triggerStringFor(triggers.head, result)
        else
          nonRegexTriggers.map(trigger => triggerStringFor(trigger, result)).mkString(" ")
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
          groupId <- result.group.id
          behaviorId <- behaviorVersion.behaviorId
        } yield {
          val url = dataService.behaviors.editLinkFor(groupId, behaviorId, lambdaService.configuration)
          s" [✎]($url)"
        }).getOrElse("")
        Some(s"$triggersString$linkText\n\n")
      }
    }
  }

  private def triggerStringFor(trigger: BehaviorTriggerData, helpResult: HelpResult): String = {
    if (helpResult.matchingTriggers.contains(trigger)) {
      s"**`${trigger.text}`**"
    } else {
      s"`${trigger.text}`"
    }
  }

  private def maybeHelpSearch: Option[String] = {
    maybeHelpString.filter(_.trim.nonEmpty)
  }

  private def matchString: String = {
    maybeHelpSearch.map { s =>
      s" related to `$s`"
    }.getOrElse("")
  }

  private def flattenUnnamedBehaviorGroupData(untitledGroups: Seq[BehaviorGroupData]): BehaviorGroupData = {
    BehaviorGroupData(
      id = None,
      event.teamId,
      name = Some("Miscellaneous"),
      description = None,
      icon = None,
      actionInputs = untitledGroups.flatMap(_.actionInputs),
      dataTypeInputs = untitledGroups.flatMap(_.dataTypeInputs),
      behaviorVersions = untitledGroups.flatMap(_.behaviorVersions),
      githubUrl = None,
      exportId = None,
      Some(OffsetDateTime.now)
    )
  }

  private def introResultFor(results: Seq[HelpResult], startAt: Int): BotResult = {
    val endAt = startAt + SlackMessageSender.MAX_ACTIONS_PER_ATTACHMENT - 1
    val resultsToShow = results.slice(startAt, endAt)
    val resultsRemaining = results.slice(endAt, results.length)

    val intro = if (startAt == 0 && maybeHelpString.isEmpty) {
      s"OK, let’s start from the top. Here are some things I know about. ${event.skillListLinkFor(lambdaService)}"
    } else if (startAt == 0 && maybeHelpString.isDefined) {
      s"OK, here’s what I know$matchString. ${event.skillListLinkFor(lambdaService)}"
    } else {
      s"OK, here are some more things I know$matchString."
    }
    val maybeInstructions = if (startAt > 0 || !isFirstTrigger) {
      None
    } else if (matchString.isEmpty) {
      Some(s"Click a skill to learn more. You can also search by keyword. For example, type:  \n`${event.botPrefix}help bananas`")
    } else {
      Some("Click a skill to learn more, or try searching a different keyword.")
    }
    val skillActions = resultsToShow.map(result => {
      val group = result.group
      val label = group.name.getOrElse("")
      val helpActionValue = group.id.getOrElse("(untitled)")
      maybeHelpSearch.map { helpSearch =>
        SlackMessageAction("help_for_skill", label, s"id=$helpActionValue&search=$helpSearch")
      }.getOrElse {
        SlackMessageAction("help_for_skill", label, helpActionValue)
      }
    })
    val remainingGroupCount = resultsRemaining.length
    val actions = if (remainingGroupCount > 0) {
      val label = if (remainingGroupCount == 1) { "1 more skill…" } else { s"$remainingGroupCount more skills…" }
      skillActions :+ SlackMessageAction("help_index", label, endAt.toString, maybeStyle = Some("primary"))
    } else {
      skillActions
    }
    val attachment = SlackMessageActions("help_index", actions, maybeInstructions, Some(Color.PINK))
    TextWithActionsResult(event, intro, forcePrivateResponse = false, attachment)
  }

  private def actionHeadingFor(group: BehaviorGroupData): String = {
    val numActions = group.behaviorVersions.filterNot(version => version.isDataType).length
    if (numActions == 0) {
      "This skill has no actions."
    } else if (numActions == 1) {
      "_**1 action:**_  "
    } else {
      s"_**$numActions actions:**_  "
    }
  }

  private def filterBehaviorVersionsIfMiscGroup(group: BehaviorGroupData, matchingItems: Seq[FuzzyMatchable]): BehaviorGroupData = {
    if (group.id.isEmpty || group.name.isEmpty) {
      val matchingBehaviorVersions = group.behaviorVersions.filter(_.triggers.exists(matchingItems.contains))
      group.copy(behaviorVersions = matchingBehaviorVersions)
    } else {
      group
    }
  }

  private def descriptionFor(result: HelpResult): String = {
    result.group.description
      .filter(_.trim.nonEmpty)
      .map { rawDescription =>
        val description = if(result.descriptionMatches) {
          maybeHelpSearch.map(helpSearch => searchPatternFor(helpSearch).replaceAllIn(result.group.description.getOrElse(""), "$1**$2**$3"))
        } else {
          rawDescription
        }
        description + "\n\n"
      }.getOrElse("")
  }

  def skillResultFor(result: HelpResult): BotResult = {

    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString. ${event.skillListLinkFor(lambdaService)}"
    } else {
      "OK, here’s the help you asked for:"
    }

    val group = result.group
    val name = group.name
      .filterNot(name => group.id.isEmpty || name.trim.isEmpty)
      .map(name => s"**$name**")
      .getOrElse("**Miscellaneous skills**")

    val actionList = group.behaviorVersions.flatMap(version => helpStringFor(result, version)).mkString("")

    val resultText =
      s"""$intro
         |
         |$name  \n${descriptionFor(result)}${actionHeadingFor(group)}
         |$actionList
         |""".stripMargin
    val actions = Seq(SlackMessageAction("help_index", "More help…", "0"))
    TextWithActionsResult(event, resultText, forcePrivateResponse = false, SlackMessageActions("help_for_skill", actions, None, Some(Color.BLUE_LIGHT), None))
  }

  def emptyResult: BotResult = {
    val actions = Seq(SlackMessageAction("help_index", "More help…", "0"))
    val resultText = s"I don’t know anything$matchString. ${event.skillListLinkFor(lambdaService)}"
    TextWithActionsResult(event, resultText, forcePrivateResponse = false, SlackMessageActions("help_no_result", actions, None, Some(Color.PINK)))
  }

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      user <- event.ensureUser(dataService)
      maybeBehaviorGroups <- maybeTeam.map { team =>
        maybeSkillId match {
          case Some("(untitled)") => dataService.behaviorGroups.allWithNoNameFor(team).map(Some(_))
          case Some(skillId) => dataService.behaviorGroups.find(skillId).map(_.map(Seq(_)))
          case None => dataService.behaviorGroups.allFor(team).map(Some(_))
        }
      }.getOrElse {
        Future.successful(None)
      }
      groupData <- maybeBehaviorGroups.map { groups =>
        Future.sequence(groups.map { group =>
          BehaviorGroupData.maybeFor(group.id, user, None, dataService)
        }).map(_.flatten.sorted)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val (named, unnamed) = groupData.partition(_.maybeNonEmptyName.isDefined)
      val flattenedGroupData = ArrayBuffer[BehaviorGroupData]()
      flattenedGroupData ++= named
      if (unnamed.nonEmpty) {
        flattenedGroupData += flattenUnnamedBehaviorGroupData(unnamed)
      }
      val matchingGroupData = maybeHelpSearch.map { helpSearch =>
        FuzzyMatcher[BehaviorGroupData](helpSearch, flattenedGroupData).run.map(HelpSearchResult.apply)
      }.getOrElse(flattenedGroupData.map(SimpleHelpResult.apply))
      if (matchingGroupData.isEmpty) {
        emptyResult
      } else if (matchingGroupData.length == 1) {
        skillResultFor(matchingGroupData.head)
      } else {
        introResultFor(matchingGroupData, maybeStartAtIndex.getOrElse(0))
      }
    }
  }
}

trait HelpResult {
  val group: BehaviorGroupData
  val descriptionMatches: Boolean
  val matchingTriggers: Seq[BehaviorTriggerData]
}

case class HelpSearchResult(underlying: FuzzyMatchResult[BehaviorGroupData]) extends HelpResult {
  val group = underlying.item
  val descriptionMatches: Boolean = underlying.patterns.contains(group.fuzzyMatchDescription)
  val matchingTriggers = underlying.patterns.flatMap {
    case trigger: BehaviorTriggerData => Some(trigger)
    case _ => None
  }
}
case class SimpleHelpResult(group: BehaviorGroupData) extends HelpResult {
  val descriptionMatches: Boolean = false
  val matchingTriggers = Seq()
}
