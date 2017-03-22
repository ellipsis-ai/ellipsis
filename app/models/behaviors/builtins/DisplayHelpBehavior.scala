package models.behaviors.builtins

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.BehaviorGroupData
import models.behaviors.events._
import models.behaviors.{BotResult, TextWithActionsResult}
import models.help.{HelpResult, HelpSearchResult, SimpleHelpResult}
import services.{AWSLambdaService, DataService}
import utils._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         maybeStartAtIndex: Option[Int],
                         isFirstTrigger: Boolean,
                         event: Event,
                         lambdaService: AWSLambdaService,
                         dataService: DataService
                       ) extends BuiltinBehavior {

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
      s"OK, let’s start from the top. Here are some things I know about. ${event.skillListLinkFor(isListEmpty = false, lambdaService)}"
    } else if (startAt == 0 && maybeHelpString.isDefined) {
      s"OK, here’s what I know$matchString. ${event.skillListLinkFor(isListEmpty = false, lambdaService)}"
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
    } else {
      if (numActions == 1) {
        "_**1 action:**_"
      } else {
        s"_**$numActions actions:**_"
      } ++ " (you can type an action to trigger it)  "
    }
  }

  def skillResultFor(result: HelpResult): BotResult = {

    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString. ${event.skillListLinkFor(isListEmpty = false, lambdaService)}"
    } else {
      "OK, here’s the help you asked for:"
    }

    val group = result.group
    val name = group.name
      .filterNot(name => group.id.isEmpty || name.trim.isEmpty)
      .map(name => s"**$name**")
      .getOrElse("**Miscellaneous skills**")

    val actionList = group.behaviorVersions.flatMap(version => result.helpStringFor(version)).mkString("")

    val resultText =
      s"""$intro
         |
         |$name  \n${result.description}\n\n${actionHeadingFor(group)}
         |$actionList
         |""".stripMargin
    val actions = Seq(SlackMessageAction("help_index", "More help…", "0"))
    TextWithActionsResult(event, resultText, forcePrivateResponse = false, SlackMessageActions("help_for_skill", actions, None, Some(Color.BLUE_LIGHT), None))
  }

  def emptyResult: BotResult = {
    val actions = Seq(SlackMessageAction("help_index", "More help…", "0"))
    val resultText = s"I don’t know anything$matchString. ${event.skillListLinkFor(isListEmpty = true, lambdaService)}"
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
        FuzzyMatcher[BehaviorGroupData](helpSearch, flattenedGroupData).run.map(matchResult => HelpSearchResult(helpSearch, matchResult, dataService, lambdaService))
      }.getOrElse(flattenedGroupData.map(group => SimpleHelpResult(group, dataService, lambdaService)))
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
