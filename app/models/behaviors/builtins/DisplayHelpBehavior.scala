package models.behaviors.builtins

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorVersionData}
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import models.behaviors.{BotResult, SuccessResult, TextWithAttachmentsResult}
import models.help._
import services.caching.CacheService
import services.{AWSLambdaService, DataService, DefaultServices}
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

    val intro = if (startAt == 0 && results.isEmpty) {
      "I don’t have any skills installed right now. Try installing a skill to make me more useful."
    } else if (startAt == 0 && maybeHelpString.isEmpty) {
      s"OK, let’s start from the top. Here are some things I know about."
    } else if (startAt == 0 && maybeHelpString.isDefined) {
      s"OK, here’s what I know$matchString."
    } else {
      s"OK, here are some more things I know$matchString."
    }
    val maybeInstructions = if (startAt > 0 || !isFirstTrigger) {
      None
    } else if (matchString.isEmpty) {
      Some(
        s"Click a skill to learn more. You can also search by keyword. Example: `${botPrefix}help bananas`")
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
    val actionsGroup = SlackMessageActionsGroup(
      SHOW_HELP_INDEX,
      actionList,
      maybeInstructions,
      None,
      Some(Color.PINK),
      if (startAt == 0) { Some("Skills") } else { None }
    )
    val attachments = if (startAt == 0) {
      Seq(generalHelpGroup(botPrefix), actionsGroup)
    } else {
      Seq(actionsGroup)
    }
    TextWithAttachmentsResult(event, None, intro, forcePrivateResponse = false, attachments)
  }

  def generalHelpText(botPrefix: String): String = {
    s"""${event.navLinks(lambdaService)}
       |
       |🗣 Have feedback? Try `${botPrefix}feedback: Is this thing on?`
     """.stripMargin
  }

  def generalHelpGroup(botPrefix: String): SlackMessageAttachmentGroup = {
    SlackMessageTextAttachmentGroup(generalHelpText(botPrefix: String), None, Some("General"))
  }

  def skillNameFor(result: HelpResult): String = {
    val icon = result.group.maybeIcon.map(icon => s"$icon ").getOrElse("")
    result.group.maybeEditLink(dataService, lambdaService).map { url =>
      s"**[${icon}${result.group.name} ✎]($url)**"
    }.getOrElse {
      s"**${icon}${result.group.name}**"
    }
  }

  def prependSkillNameToText(helpResult: HelpResult, text: String): String = {
    s"${skillNameFor(helpResult)}\n$text"
  }

  def skillNameAndDescriptionFor(result: HelpResult): String = {
    if (includeNameAndDescription) {
      prependSkillNameToText(result, s"${result.description}\n\n")
    } else {
      ""
    }
  }

  private def shouldRunHelpActionFor(result: HelpResult, behaviorVersions: Seq[BehaviorVersionData]): Boolean = {
    result.group match {
      case skillGroupData: SkillHelpGroupData => {
        behaviorVersions.forall(_.name.contains("help")) ||
          maybeHelpSearch.isEmpty ||
          maybeHelpSearch.exists(_.equalsIgnoreCase(skillGroupData.name.trim))
      }
      case _ => false
    }
  }

  def skillResultFor(result: HelpResult)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val behaviorVersions = result.behaviorVersionsToDisplay(includeNonMatchingResults)
    for {
      maybeSkillResult <- if (shouldRunHelpActionFor(result, behaviorVersions)) {
        result.group.maybeGroupId.map { behaviorGroupId =>
          maybeRunHelpBehaviorFor(result, behaviorGroupId)
        }.getOrElse(Future.successful(None))
      } else {
        Future.successful(None)
      }
    } yield {
      maybeSkillResult.getOrElse {
        skillActionsListResultFor(result, behaviorVersions)
      }
    }
  }

  private def maybeRunHelpBehaviorFor(helpResult: HelpResult, behaviorGroupId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotResult]] = {
    for {
      maybeBehaviorGroup <- dataService.behaviorGroups.findWithoutAccessCheck(behaviorGroupId)
      maybeBehaviorGroupVersion <- maybeBehaviorGroup.map { group =>
        dataService.behaviorGroupVersions.maybeCurrentFor(group)
      }.getOrElse(Future.successful(None))
      behaviorVersions <- maybeBehaviorGroupVersion.map { groupVersion =>
        dataService.behaviorVersions.allForGroupVersion(groupVersion)
      }.getOrElse(Future.successful(Seq()))
      maybeBehaviorVersion <- Future.successful {
        behaviorVersions.filterNot(_.isDataType).find(_.maybeName.contains("help"))
      }
      maybeResponse <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorResponses.buildFor(event, behaviorVersion, Map(), None, None).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeResult <- maybeResponse.map { response =>
        response.result.map {
          case s: SuccessResult => {
            s.copy(maybeResponseTemplate = s.maybeResponseTemplate.map(text => prependSkillNameToText(helpResult, text)))
          }
          case otherResult => {
            otherResult
          }
        }.map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeResult
  }

  def skillActionsListResultFor(result: HelpResult, behaviorVersions: Seq[BehaviorVersionData]): BotResult = {
    val intro = if (isFirstTrigger) {
      s"Here’s what I know$matchString."
    } else {
      "OK, here’s the help you asked for:"
    }
    val versionsText = result.helpTextFor(behaviorVersions)
    val nameAndDescription = skillNameAndDescriptionFor(result)
    val listHeading = result.behaviorVersionsHeading(includeNonMatchingResults) ++ "  "
    val resultText = s"""$intro
                        |
                        |$nameAndDescription$listHeading
                        |$versionsText
                        |""".stripMargin

    val runnableActions = result.slackRunActionsFor (behaviorVersions)
    val indexAction = result.slackHelpIndexAction
    val actionList = result.maybeShowAllBehaviorVersionsAction (maybeHelpSearch, includeNonMatchingResults).map {showAllAction =>
      runnableActions ++ Seq (showAllAction, indexAction)
    } getOrElse {
      runnableActions :+ indexAction
    }
      val actionText = if (behaviorVersions.length == 1) {
      None
    } else {
      Some ("Select or type an action to run it now:")
    }

    val actionsGroup = SlackMessageActionsGroup(SHOW_BEHAVIOR_GROUP_HELP, actionList, actionText, None, Some(Color.BLUE_LIGHT), None)

    TextWithAttachmentsResult(event, None, resultText, forcePrivateResponse = false, Seq(actionsGroup))
  }

  def emptyResult(botPrefix: String): BotResult = {
    val actionList = Seq(SlackMessageActionButton(SHOW_HELP_INDEX, "More help…", "0"))
    val resultText = s"I don’t know anything$matchString."
    val actionsGroup = SlackMessageActionsGroup("help_no_result", actionList, None, None, Some(Color.PINK))
    TextWithAttachmentsResult(event, None, resultText, forcePrivateResponse = false, Seq(generalHelpGroup(botPrefix), actionsGroup))
  }

  def searchedHelp: Boolean = maybeHelpSearch.isDefined
  def clickedSkill: Boolean = maybeSkillId.isDefined
  def shouldShowSingleSkill(matchingGroupData: Seq[HelpResult]): Boolean = {
    (searchedHelp || clickedSkill) && matchingGroupData.length == 1
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
      groupData <- (for {
        channel <- event.maybeChannel
        groups <- maybeBehaviorGroups
      } yield {
        Future.sequence(groups.map { group =>
          dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(group, event.context, channel).flatMap { maybeGroupVersion =>
            maybeGroupVersion.map { groupVersion =>
              BehaviorGroupData.buildFor(groupVersion, user, None, dataService, cacheService).map(Some(_))
            }.getOrElse(Future.successful(None))
          }
        }).map(_.flatten.sorted)
      }).getOrElse(Future.successful(Seq()))
      botPrefix <- event.contextualBotPrefix(services)
      helpResult <- helpResultFor(groupData, botPrefix)
    } yield helpResult
  }

  private def helpResultFor(groupData: Seq[BehaviorGroupData], botPrefix: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
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

    if (searchedHelp && matchingGroupData.isEmpty) {
      Future.successful(emptyResult(botPrefix))
    } else if (shouldShowSingleSkill(matchingGroupData)) {
      skillResultFor(matchingGroupData.head)
    } else {
      Future.successful(introResultFor(matchingGroupData, maybeStartAtIndex.getOrElse(0), botPrefix))
    }
  }
}
