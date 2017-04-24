package models.behaviors.builtins

import akka.actor.ActorSystem
import json.BehaviorGroupData
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import models.behaviors.{BotResult, TextWithActionsResult}
import models.help._
import services.{AWSLambdaService, DataService}
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         maybeStartAtIndex: Option[Int],
                         includeNameAndDescription: Boolean,
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
      val label = group.shortName
      val helpActionValue = group.helpActionId
      maybeHelpSearch.map { helpSearch =>
        SlackMessageActionButton(SHOW_BEHAVIOR_GROUP_HELP, label, s"id=$helpActionValue&search=$helpSearch")
      }.getOrElse {
        SlackMessageActionButton(SHOW_BEHAVIOR_GROUP_HELP, label, helpActionValue)
      }
    })
    val remainingGroupCount = resultsRemaining.length
    val actions = if (remainingGroupCount > 0) {
      val label = if (remainingGroupCount == 1) { "1 more skill…" } else { s"$remainingGroupCount more skills…" }
      skillActions :+ SlackMessageActionButton(SHOW_HELP_INDEX, label, endAt.toString, maybeStyle = Some("primary"))
    } else {
      skillActions
    }
    val attachment = SlackMessageActions(SHOW_HELP_INDEX, actions, maybeInstructions, Some(Color.PINK))
    TextWithActionsResult(event, None, intro, forcePrivateResponse = false, attachment)
  }

  def skillResultFor(result: HelpResult): BotResult = {

    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString. ${event.skillListLinkFor(isListEmpty = false, lambdaService)}"
    } else {
      "OK, here’s the help you asked for:"
    }

    val group = result.group
    val skillNameAndDescription = if (includeNameAndDescription) {
      val name = s"**${group.name}**"
      val description = result.description
      s"$name  \n$description\n\n"
    } else {
      ""
    }

    val sortedBehaviorVersions = result.behaviorVersionsToDisplay
    val versionsText = result.helpText
    val runnableActions = result.slackRunActions

    val resultText =
      s"""$intro
         |
         |$skillNameAndDescription${result.behaviorVersionsHeading ++ "  "}
         |$versionsText
         |""".stripMargin
    val actions = runnableActions ++ Seq(result.maybeShowAllBehaviorVersionsAction, Some(result.slackHelpIndexAction)).flatten
    val actionText = if (sortedBehaviorVersions.length == 1) { None } else { Some("Select or type an action to run it now:") }
    val messageActions = SlackMessageActions(SHOW_BEHAVIOR_GROUP_HELP, actions, actionText, Some(Color.BLUE_LIGHT), None)
    TextWithActionsResult(event, None, resultText, forcePrivateResponse = false, messageActions)
  }

  def emptyResult: BotResult = {
    val actions = Seq(SlackMessageActionButton(SHOW_HELP_INDEX, "More help…", "0"))
    val resultText = s"I don’t know anything$matchString. ${event.skillListLinkFor(isListEmpty = true, lambdaService)}"
    TextWithActionsResult(event, None, resultText, forcePrivateResponse = false, SlackMessageActions("help_no_result", actions, None, Some(Color.PINK)))
  }

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      user <- event.ensureUser(dataService)
      maybeBehaviorGroups <- maybeTeam.map { team =>
        maybeSkillId match {
          case Some(HelpGroupData.MISCELLANEOUS_ACTION_ID) => dataService.behaviorGroups.allWithNoNameFor(team).map(Some(_))
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
      val namedGroupData = named.map(behaviorGroupData => SkillHelpGroupData(behaviorGroupData))
      val flattenedGroupData = if (unnamed.nonEmpty) {
        namedGroupData :+ MiscHelpGroupData(unnamed)
      } else {
        namedGroupData
      }
      val matchingGroupData = maybeHelpSearch.map { helpSearch =>
        FuzzyMatcher[HelpGroupData](helpSearch, flattenedGroupData).run.map(matchResult => HelpSearchResult(helpSearch, matchResult, event, dataService, lambdaService))
      }.getOrElse(flattenedGroupData.map(group => SimpleHelpResult(group, event, dataService, lambdaService)))
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
