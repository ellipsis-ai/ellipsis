package models.behaviors.builtins

import akka.actor.ActorSystem
import json.BehaviorGroupData
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import models.behaviors.{BotResult, TextWithAttachmentsResult}
import models.help._
import services.{AWSLambdaService, CacheService, DataService, DefaultServices}
import utils._

import scala.concurrent.{ExecutionContext, Future}

case class DisplayHelpBehavior(
                         maybeHelpString: Option[String],
                         maybeSkillId: Option[String],
                         maybeStartAtIndex: Option[Int],
                         includeNameAndDescription: Boolean,
                         includeNonMatchingResults: Boolean,
                         isFirstTrigger: Boolean,
                         event: Event,
                         services: DefaultServices
                       ) extends BuiltinBehavior {

  val lambdaService: AWSLambdaService = services.lambdaService
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  private def maybeHelpSearch: Option[String] = {
    maybeHelpString.filter(_.trim.nonEmpty)
  }

  private def matchString: String = {
    maybeHelpSearch.map { s =>
      s" related to “$s”"
    }.getOrElse("")
  }

  private def introResultFor(results: Seq[HelpResult], startAt: Int, botPrefix: String): BotResult = {
    val endAt = startAt + SlackMessageSender.MAX_ACTIONS_PER_ATTACHMENT - 1
    val resultsToShow = results.slice(startAt, endAt)
    val resultsRemaining = results.slice(endAt, results.length)

    val intro = if (startAt == 0 && maybeHelpString.isEmpty) {
      s"OK, let’s start from the top. Here are some things I know about."
    } else if (startAt == 0 && maybeHelpString.isDefined) {
      s"OK, here’s what I know$matchString."
    } else {
      s"OK, here are some more things I know$matchString."
    }
    val maybeInstructions = if (startAt > 0 || !isFirstTrigger) {
      None
    } else if (matchString.isEmpty) {
      Some(s"Click a skill to learn more. You can also search by keyword, e.g. “${botPrefix}help bananas”")
    } else {
      Some("Click a skill to learn more, or try searching a different keyword.")
    }
    val skillActions = resultsToShow.map(result => {
      val label = result.group.shortName
      val buttonValue = HelpGroupSearchValue(result.group.helpActionId, maybeHelpSearch).toString
      SlackMessageActionButton(SHOW_BEHAVIOR_GROUP_HELP, label, buttonValue)
    })

    val remainingGroupCount = resultsRemaining.length
    val actionList = if (remainingGroupCount > 0) {
      val label = if (remainingGroupCount == 1) { "1 more skill…" } else { s"$remainingGroupCount more skills…" }
      skillActions :+ SlackMessageActionButton(SHOW_HELP_INDEX, label, endAt.toString, maybeStyle = Some("primary"))
    } else {
      skillActions
    }
    val actions = SlackMessageActions(
      SHOW_HELP_INDEX,
      actionList,
      maybeInstructions,
      Some(Color.PINK),
      if (startAt == 0) { Some("Skills") } else { None },
      event.maybeSkillsListLinkFor(lambdaService)
    )
    val attachments = if (startAt == 0) {
      val generalHelp = SlackMessageTextAttachments(event.navLinks(lambdaService), Some("General"))
      Seq(actions, generalHelp)
    } else {
      Seq(actions)
    }
    TextWithAttachmentsResult(event, None, intro, forcePrivateResponse = false, attachments)
  }

  def skillNameAndDescriptionFor(result: HelpResult): String = {
    if (includeNameAndDescription) {
      val name = result.group.maybeEditLink(dataService, lambdaService).map { url =>
        s"**[${result.group.name} ✎]($url)**"
      }.getOrElse {
        s"**${result.group.name}**"
      }
      val description = result.description
      s"$name  \n$description\n\n"
    } else {
      ""
    }
  }

  def skillResultFor(result: HelpResult): BotResult = {
    val behaviorVersions = result.behaviorVersionsToDisplay(includeNonMatchingResults)

    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString."
    } else {
      "OK, here’s the help you asked for:"
    }
    val versionsText = result.helpTextFor(behaviorVersions)
    val nameAndDescription = skillNameAndDescriptionFor(result)
    val listHeading = result.behaviorVersionsHeading(includeNonMatchingResults) ++ "  "
    val resultText =
      s"""$intro
         |
         |$nameAndDescription$listHeading
         |$versionsText
         |""".stripMargin

    val runnableActions = result.slackRunActionsFor(behaviorVersions)
    val indexAction = result.slackHelpIndexAction
    val actionList = result.maybeShowAllBehaviorVersionsAction(maybeHelpSearch, includeNonMatchingResults).map { showAllAction =>
      runnableActions ++ Seq(showAllAction, indexAction)
    } getOrElse {
      runnableActions :+ indexAction
    }
    val actionText = if (behaviorVersions.length == 1) {
      None
    } else {
      Some("Select or type an action to run it now:")
    }

    val actions = SlackMessageActions(SHOW_BEHAVIOR_GROUP_HELP, actionList, actionText, Some(Color.BLUE_LIGHT), None)

    TextWithAttachmentsResult(event, None, resultText, forcePrivateResponse = false, Seq(actions))
  }

  def emptyResult: BotResult = {
    val actionList = Seq(SlackMessageActionButton(SHOW_HELP_INDEX, "More help…", "0"))
    val resultText = s"I don’t know anything$matchString."
    val actions = SlackMessageActions("help_no_result", actionList, None, Some(Color.PINK))
    TextWithAttachmentsResult(event, None, resultText, forcePrivateResponse = false, Seq(actions))
  }

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      user <- event.ensureUser(dataService)
      maybeBehaviorGroups <- maybeTeam.map { team =>
        maybeSkillId match {
          case Some(HelpGroupData.MISCELLANEOUS_ACTION_ID) => dataService.behaviorGroups.allWithNoNameFor(team).map(Some(_))
          case Some(skillId) => dataService.behaviorGroups.findWithoutAccessCheck(skillId).map(_.map(Seq(_)))
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
      botPrefix <- event.botPrefix(services)
    } yield {
      val (named, unnamed) = groupData.partition(_.maybeNonEmptyName.isDefined)
      val namedGroupData = named.map(behaviorGroupData => SkillHelpGroupData(behaviorGroupData))
      val flattenedGroupData = if (unnamed.nonEmpty) {
        namedGroupData :+ MiscHelpGroupData(unnamed)
      } else {
        namedGroupData
      }
      val matchingGroupData = maybeHelpSearch.map { helpSearch =>
        FuzzyMatcher[HelpGroupData](helpSearch, flattenedGroupData).run.map(matchResult => HelpSearchResult(helpSearch, matchResult, event, dataService, lambdaService, botPrefix))
      }.getOrElse(flattenedGroupData.map(group => SimpleHelpResult(group, event, dataService, lambdaService, botPrefix)))
      if (matchingGroupData.isEmpty) {
        emptyResult
      } else if (matchingGroupData.length == 1) {
        skillResultFor(matchingGroupData.head)
      } else {
        introResultFor(matchingGroupData, maybeStartAtIndex.getOrElse(0), botPrefix)
      }
    }
  }
}
