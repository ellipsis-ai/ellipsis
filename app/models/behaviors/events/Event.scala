package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion}
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.ellipsisobject.{BotInfo, MessageInfo, UserInfo}
import models.behaviors.scheduling.Scheduled
import models.behaviors.triggers.Trigger
import models.team.Team
import play.api.libs.json.JsObject
import services.{AWSLambdaService, DataService, DefaultServices}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}

trait Event {
  type EC <: EventContext
  val eventContext: EC
  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    eventContext.maybeBotInfo(services)
  }
  val ellipsisTeamId: String = eventContext.ellipsisTeamId
  val maybeChannel: Option[String] = eventContext.maybeChannel
  lazy val maybeThreadId: Option[String] = eventContext.maybeThreadId
  val messageText: String
  val maybeMessageIdForReaction: Option[String]
  val relevantMessageText: String = messageText
  val relevantMessageTextWithFormatting: String = messageText
  lazy val maybeMessageText: Option[String] = Option(messageText).filter(_.trim.nonEmpty)
  val maybeScheduled: Option[Scheduled] = None
  val eventType: EventType
  val maybeOriginalEventType: Option[EventType]
  val isResponseExpected: Boolean
  val includesBotMention: Boolean
  val messageRecipientPrefix: String = eventContext.messageRecipientPrefix(isUninterruptedConversation)
  val isPublicChannel: Boolean = eventContext.isPublicChannel
  val isUninterruptedConversation: Boolean = false
  val isEphemeral: Boolean = false
  val maybeResponseUrl: Option[String] = None
  val isBotMessage: Boolean = false
  val beQuiet: Boolean = false
  val maybeReactionAdded: Option[String] = None

  def hasFile: Boolean = false

  def originalEventType: EventType = {
    maybeOriginalEventType.getOrElse(eventType)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterruptedConversation: Boolean): Event

  def logTextForResultSource: String = s"in response to ${eventContext.name} message"

  def logTextFor(result: BotResult, maybeSource: Option[String]): String = {
    val channelText = maybeChannel.map { channel =>
      s" in channel [${channel}]"
    }.getOrElse("")
    val contextUserText = s" for context user ID [$result.event.eventContext.userIdForContext]"
    val convoText = result.maybeConversation.map { convo =>
      s" in conversation [${convo.id}]"
    }.getOrElse("")
    val sourceText = maybeSource.getOrElse(logTextForResultSource)
    val logIntro = s"Sending result $sourceText [${messageText}]$channelText$contextUserText$convoText: [${result.fullText}]"
    s"$logIntro\n${result.filesAsLogText}"
  }

  def ensureUserAction(dataService: DataService): DBIO[User] = eventContext.ensureUserAction(dataService)

  def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    dataService.run(ensureUserAction(dataService))
  }

  def userInfoAction(maybeConversation: Option[Conversation], services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    UserInfo.buildForAction(this, maybeConversation, services)
  }

  def messageInfo(maybeConversation: Option[Conversation], services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[MessageInfo] = {
    MessageInfo.buildFor(this, maybeConversation, services)
  }

  def messageUserDataList: Set[MessageUserData]

  def messageUserDataList(maybeConversation: Option[Conversation], services: DefaultServices): Set[MessageUserData] = {
    messageUserDataList ++ maybeConversation.flatMap { conversation =>
      services.cacheService.getMessageUserDataList(conversation.id)
    }.getOrElse(Seq.empty)
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    eventContext.detailsFor(services)
  }

  def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(None)
  }

  def navLinkList(lambdaService: AWSLambdaService): Seq[(String, String)] = {
    lambdaService.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val skillsListPath = baseUrl + controllers.routes.ApplicationController.index(Some(ellipsisTeamId))
      val schedulingPath = baseUrl + controllers.routes.ScheduledActionsController.index(None, None, Some(ellipsisTeamId))
      val settingsPath = baseUrl + controllers.web.settings.routes.EnvironmentVariablesController.list(Some(ellipsisTeamId))
      Seq(
        "View and install skills" -> skillsListPath,
        "Scheduling" -> schedulingPath,
        "Team settings" -> settingsPath
      )
    }.getOrElse(Seq())
  }

  def navLinks(lambdaService: AWSLambdaService): String = {
    navLinkList(lambdaService).map { case(title, path) =>
      s"[$title]($path)"
    }.mkString("  ·  ")
  }

  def teachMeLinkFor(lambdaService: AWSLambdaService): String = {
    val newBehaviorLink = lambdaService.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.BehaviorEditorController.newGroup(Some(ellipsisTeamId))
      s"$baseUrl$path"
    }.get
    s"[teach me something new]($newBehaviorLink)"
  }

  def installLinkFor(lambdaService: AWSLambdaService): String = {
    val installLink = lambdaService.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.index(Some(ellipsisTeamId))
      s"$baseUrl$path"
    }.get
    s"[install new skills]($installLink)"
  }

  def noExactMatchResult(services: DefaultServices)
                        (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    DisplayHelpBehavior(
      Some(relevantMessageText),
      None,
      Some(0),
      includeNameAndDescription = true,
      includeNonMatchingResults = false,
      isFirstTrigger = true,
      this,
      services
    ).result
  }

  def shouldAutoForcePrivate(behaviorVersion: BehaviorVersion, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    dataService.behaviorParameters.allFor(behaviorVersion).map { params =>
      isEphemeral && params.exists(_.paramType.mayRequireTypedAnswer)
    }
  }
  def maybeChannelToUseFor(behaviorVersion: BehaviorVersion, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    for {
      forcePrivate <- shouldAutoForcePrivate(behaviorVersion, services.dataService).map(_ || behaviorVersion.forcePrivateResponse)
      maybeChannelToUse <- if (forcePrivate) {
        eventContext.eventualMaybeDMChannel(services).map { maybeDMChannel =>
          maybeDMChannel
        }
      } else {
        Future.successful(maybeChannel)
      }
    } yield maybeChannelToUse
  }

  def resultReactionHandler(eventualResults: Future[Seq[BotResult]], services: DefaultServices)
                           (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    eventContext.reactionHandler(eventualResults, maybeMessageIdForReaction, services)
  }

  def sendMessage(
                   text: String,
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    eventContext.sendMessage(this, text, responseType, maybeShouldUnfurl, maybeConversation, attachments, files, choices, developerContext, services)
  }

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    eventContext.botName(services)
  }

  def contextualBotPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = Future.successful("")

  val invocationLogText: String

  def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = Future.successful(None)

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]]

  def activatedTriggersIn(
                           triggers: Seq[Trigger],
                           dataService: DataService
                         )(implicit ec: ExecutionContext): Future[Seq[Trigger]] = {
    val activatedTriggerLists = triggers.
        filter(_.isActivatedBy(this)).
        groupBy(_.behaviorVersion).
        values.
        toSeq
    Future.sequence(
      activatedTriggerLists.map { list =>
        Future.sequence(list.map { trigger =>
          for {
            params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          } yield {
            (trigger, trigger.invocationParamsFor(this, params).size)
          }
        })
      }
    ).map { activatedTriggerListsWithParamCounts =>

      // we want to chose activated triggers with more params first
      activatedTriggerListsWithParamCounts.flatMap { list =>
        list.
          sortBy { case(_, paramCount) => paramCount }.
          map { case(trigger, _) => trigger }.
          reverse.
          headOption
      }
    }
  }

}
