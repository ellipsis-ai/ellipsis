package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion}
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduling.Scheduled
import models.behaviors.triggers.Trigger
import models.team.Team
import play.api.Configuration
import play.api.libs.json.JsObject
import services.{AWSLambdaService, DataService, DefaultServices}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}

trait Event {
  val name: String
  val userIdForContext: String
  val botUserIdForContext: String
  val teamId: String
  val maybeChannel: Option[String]
  val maybeThreadId: Option[String]
  val messageText: String
  val relevantMessageText: String = messageText
  val relevantMessageTextWithFormatting: String = messageText
  lazy val maybeMessageText: Option[String] = Option(messageText).filter(_.trim.nonEmpty)
  val maybeScheduled: Option[Scheduled] = None
  val eventType: EventType
  val maybeOriginalEventType: Option[EventType]
  val context = name
  val isResponseExpected: Boolean
  val includesBotMention: Boolean
  val messageRecipientPrefix: String
  val isPublicChannel: Boolean
  val isUninterruptedConversation: Boolean = false
  val isEphemeral: Boolean = false
  val maybeResponseUrl: Option[String] = None

  def originalEventType: EventType = {
    maybeOriginalEventType.getOrElse(eventType)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterruptedConversation: Boolean): Event

  def logTextForResultSource: String = "in response to slack message"

  def logTextFor(result: BotResult, maybeSource: Option[String]): String = {
    val channelText = maybeChannel.map { channel =>
      s" in channel [${channel}]"
    }.getOrElse("")
    val userText = s" for context user ID [${result.event.userIdForContext}]"
    val convoText = result.maybeConversation.map { convo =>
      s" in conversation [${convo.id}]"
    }.getOrElse("")
    val sourceText = maybeSource.getOrElse(logTextForResultSource)
    val logIntro = s"Sending result $sourceText [${messageText}]$channelText$userText$convoText: [${result.fullText}]"
    s"$logIntro\n${result.filesAsLogText}"
  }

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUserAction(dataService: DataService): DBIO[User] = {
    dataService.users.ensureUserForAction(loginInfo, teamId)
  }

  def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    dataService.run(ensureUserAction(dataService))
  }

  def userInfoAction(maybeConversation: Option[Conversation], services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    UserInfo.buildForAction(this, maybeConversation, teamId, services)
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

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject]

  def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(None)
  }

  def navLinkList(lambdaService: AWSLambdaService): Seq[(String, String)] = {
    lambdaService.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val skillsListPath = baseUrl + controllers.routes.ApplicationController.index(Some(teamId))
      val schedulingPath = baseUrl + controllers.routes.ScheduledActionsController.index(None, None, Some(teamId))
      val settingsPath = baseUrl + controllers.web.settings.routes.EnvironmentVariablesController.list(Some(teamId))
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
      val path = controllers.routes.BehaviorEditorController.newGroup(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[teach me something new]($newBehaviorLink)"
  }

  def installLinkFor(lambdaService: AWSLambdaService): String = {
    val installLink = lambdaService.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.index(Some(teamId))
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

  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def shouldAutoForcePrivate(behaviorVersion: BehaviorVersion, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    dataService.behaviorParameters.allFor(behaviorVersion).map { params =>
      isEphemeral && params.exists(_.paramType.mayRequireTypedAnswer)
    }
  }
  def maybeChannelToUseFor(behaviorVersion: BehaviorVersion, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    for {
      forcePrivate <- shouldAutoForcePrivate(behaviorVersion, services.dataService).map(_ || behaviorVersion.forcePrivateResponse)
      maybeChannelToUse <- if (forcePrivate) {
        eventualMaybeDMChannel(services).map { maybeDMChannel =>
          maybeDMChannel
        }
      } else {
        Future.successful(maybeChannel)
      }
    } yield maybeChannelToUse
  }

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]]

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]]

  def resultReactionHandler(eventualResults: Future[Seq[BotResult]], services: DefaultServices)
                           (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = eventualResults

  def sendMessage(
                   text: String,
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices,
                   configuration: Configuration
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String]

  def contextualBotPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = Future.successful("")

  val invocationLogText: String

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
