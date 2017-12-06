package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduling.Scheduled
import models.team.Team
import play.api.libs.json.JsObject
import services.{AWSLambdaService, CacheService, DataService, DefaultServices}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}

trait Event {
  val name: String
  val userIdForContext: String
  val teamId: String
  val maybeChannel: Option[String]
  val maybeThreadId: Option[String]
  val messageText: String
  val relevantMessageText: String = messageText
  val relevantMessageTextWithFormatting: String = messageText
  val maybeMessageText: Option[String] = Option(messageText).filter(_.trim.nonEmpty)
  val maybeScheduled: Option[Scheduled] = None
  val eventType: EventType
  val maybeOriginalEventType: Option[EventType]
  val context = name
  val isResponseExpected: Boolean
  val includesBotMention: Boolean
  val messageRecipientPrefix: String
  val isPublicChannel: Boolean

  def originalEventType: EventType = {
    maybeOriginalEventType.getOrElse(eventType)
  }

  def withOriginalEventType(originalEventType: EventType): Event

  def logTextForResultSource: String = "in response to slack message"

  def logTextFor(result: BotResult, maybeSource: Option[String]): String = {
    val channelText = maybeChannel.map { channel =>
      s" in channel [${channel}]"
    }.getOrElse("")
    val convoText = result.maybeConversation.map { convo =>
      s" in conversation [${convo.id}]"
    }.getOrElse("")
    val sourceText = maybeSource.getOrElse(logTextForResultSource)
    val logIntro = s"Sending result [${result.fullText}] $sourceText [${messageText}]$channelText$convoText"
    if (result.files.nonEmpty) {
      val files = result.files.map { fileSpec =>
        val filename = fileSpec.filename.getOrElse("File")
        val filetype = fileSpec.filetype.getOrElse("unknown type")
        val content = fileSpec.content.map { content =>
          val lines = content.split("\n")
          if (lines.length > 10) {
            lines.slice(0, 10).mkString("", "\n", "\n...(truncated)")
          } else {
            lines.mkString("\n")
          }
        }.getOrElse("(empty)")
        s"""$filename ($filetype):
           |$content
         """.stripMargin
      }
      val fileString = files.mkString("\n======\n")
      s"""$logIntro
         |======
         |Files:
         |======
         |$fileString
         |======
         |""".stripMargin
    } else {
      logIntro
    }
  }

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUserAction(dataService: DataService): DBIO[User] = {
    dataService.users.ensureUserForAction(loginInfo, teamId)
  }

  def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    dataService.run(ensureUserAction(dataService))
  }

  def userInfoAction(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    UserInfo.buildForAction(this, teamId, services)
  }

  def messageInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[MessageInfo] = {
    MessageInfo.buildFor(this, services)
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject]

  def recentMessages(dataService: DataService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[String]] = Future.successful(Seq())

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
      s"$title: $path"
    }.mkString("\n")
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
      Some(messageText),
      None,
      Some(0),
      includeNameAndDescription = true,
      includeNonMatchingResults = false,
      isFirstTrigger = true,
      this,
      services
    ).result
  }

  def eventualMaybeDMChannel(cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def maybeChannelToUseFor(behaviorVersion: BehaviorVersion, cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    eventualMaybeDMChannel(cacheService).map { maybeDMChannel =>
      if (behaviorVersion.forcePrivateResponse) {
        maybeDMChannel
      } else {
        maybeChannel
      }
    }
  }

  def maybeChannelForSendAction(
                                 forcePrivate: Boolean,
                                 maybeConversation: Option[Conversation],
                                 dataService: DataService
                               )(implicit actorSystem: ActorSystem): DBIO[Option[String]] = {
    DBIO.successful(maybeChannel)
  }

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]]

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   cacheService: CacheService
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def botPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = Future.successful("")

  val invocationLogText: String

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]]

}
