package models.behaviors.builtins

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.{MessageEvent, SlackMessageAction, SlackMessageActions, SlackMessageEvent}
import models.behaviors.triggers.TriggerFuzzyMatcher
import models.behaviors.{BotResult, TextWithActionsResult}
import services.{AWSLambdaService, DataService}
import utils.Color

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         maybeStartAtIndex: Option[Int],
                         isFirstTrigger: Boolean,
                         event: MessageEvent,
                         lambdaService: AWSLambdaService,
                         dataService: DataService
                       ) extends BuiltinBehavior {

  private def helpStringFor(behaviorVersion: BehaviorVersionData, maybeMatchingTriggers: Option[Seq[BehaviorTriggerData]]): Option[String] = {
    val triggers = behaviorVersion.triggers
    if (triggers.isEmpty) {
      None
    } else {
      val nonRegexTriggers = triggers.filterNot(_.isRegex)
      val namedTriggers =
        if (nonRegexTriggers.isEmpty)
          triggerStringFor(triggers.head, maybeMatchingTriggers)
        else
          nonRegexTriggers.map(trigger => triggerStringFor(trigger, maybeMatchingTriggers)).mkString(" ")
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
        Some(s"$triggersString$link\n\n")
      }
    }
  }

  private def triggerStringFor(trigger: BehaviorTriggerData, maybeMatchingTriggers: Option[Seq[BehaviorTriggerData]]): String = {
    if (maybeMatchingTriggers.exists(_.contains(trigger))) {
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
      name = "Miscellaneous",
      description = "",
      icon = None,
      actionInputs = untitledGroups.flatMap(_.actionInputs),
      dataTypeInputs = untitledGroups.flatMap(_.dataTypeInputs),
      behaviorVersions = untitledGroups.flatMap(_.behaviorVersions),
      githubUrl = None,
      importedId = None,
      publishedId = None,
      OffsetDateTime.now
    )
  }

  private def introResultFor(groupData: Seq[BehaviorGroupData], startAt: Int): BotResult = {
    val endAt = startAt + SlackMessageEvent.MAX_ACTIONS_PER_ATTACHMENT - 1
    val groupsToShow = groupData.slice(startAt, endAt)
    val groupsRemaining = groupData.slice(endAt, groupData.length)

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
    val skillActions = groupsToShow.map(group => {
      val label = group.name
      val helpActionValue = group.id.getOrElse("(untitled)")
      maybeHelpSearch.map { helpSearch =>
        SlackMessageAction("help_for_skill", label, s"id=$helpActionValue&search=$helpSearch")
      }.getOrElse {
        SlackMessageAction("help_for_skill", label, helpActionValue)
      }
    })
    val remainingGroupCount = groupsRemaining.length
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
    val numActions = group.behaviorVersions.length
    if (numActions == 0) {
      "This skill has no actions."
    } else if (numActions == 1) {
      "_**1 action:**_  "
    } else {
      s"_**$numActions actions:**_  "
    }
  }

  private def filterBehaviorVersionsIfMiscGroup(group: BehaviorGroupData, allMatchingTriggers: Seq[BehaviorTriggerData]): BehaviorGroupData = {
    if (group.id.isEmpty || group.name.isEmpty) {
      val matchingBehaviorVersions = group.behaviorVersions.filter(_.triggers.exists(allMatchingTriggers.contains))
      group.copy(behaviorVersions = matchingBehaviorVersions)
    } else {
      group
    }
  }

  def skillResultFor(group: BehaviorGroupData, maybeMatchingTriggers: Option[Seq[BehaviorTriggerData]]): BotResult = {

    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString. ${event.skillListLinkFor(lambdaService)}"
    } else {
      "OK, here’s the help you asked for:"
    }

    val name = if (group.id.isEmpty) {
      "**Miscellaneous skills**"
    } else {
      s"**${group.name}**"
    }

    val description = if (group.description.isEmpty) {
      ""
    } else {
      s"${group.description}\n\n"
    }

    val actionList = group.behaviorVersions.flatMap(version => helpStringFor(version, maybeMatchingTriggers)).mkString("")

    val resultText =
      s"""$intro
         |
         |$name  \n$description${actionHeadingFor(group)}
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
          case Some("(untitled)") =>
            dataService.behaviorGroups.allFor(team).map(_.filter(_.name.isEmpty)).map(Some(_))
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
      val (named, unnamed) = groupData.partition(_.name.nonEmpty)
      val flattenedGroupData = named :+ flattenUnnamedBehaviorGroupData(unnamed)
      val maybeMatchingTriggers = maybeHelpSearch.map { helpSearch =>
        val allTriggers = flattenedGroupData.flatMap(_.behaviorVersions).flatMap(_.triggers)
        TriggerFuzzyMatcher(helpSearch, allTriggers).run.map(_._1)
      }
      val matchingGroupData = maybeMatchingTriggers.map { matchingTriggers =>
        flattenedGroupData.
          filter { group =>
            maybeHelpSearch.exists(group.nameOrDescriptionContains) ||
              group.behaviorVersions.exists(_.triggers.exists(matchingTriggers.contains))
          }.
          map(group => filterBehaviorVersionsIfMiscGroup(group, matchingTriggers))
      }.getOrElse(flattenedGroupData)
      if (matchingGroupData.isEmpty) {
        emptyResult
      } else if (matchingGroupData.length == 1) {
        skillResultFor(matchingGroupData.head, maybeMatchingTriggers)
      } else {
        introResultFor(matchingGroupData, maybeStartAtIndex.getOrElse(0))
      }
    }
  }
}
