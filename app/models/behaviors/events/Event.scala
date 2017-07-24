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
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait Event {
  val name: String
  val userIdForContext: String
  val teamId: String
  val maybeChannel: Option[String]
  val maybeThreadId: Option[String]
  val messageText: String
  val relevantMessageText: String = messageText
  val maybeMessageText: Option[String] = Option(messageText).filter(_.trim.nonEmpty)
  val maybeScheduled: Option[Scheduled] = None
  val context = name
  val isResponseExpected: Boolean
  val includesBotMention: Boolean
  val messageRecipientPrefix: String
  val isPublicChannel: Boolean

  def logTextFor(result: BotResult): String = {
    val channelText = maybeChannel.map { channel =>
      s" in channel [${channel}]"
    }.getOrElse("")
    val convoText = result.maybeConversation.map { convo =>
      s" in conversation [${convo.id}]"
    }.getOrElse("")
    s"Sending result [${result.fullText}] in response to slack message [${messageText}]$channelText$convoText"
  }

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUserAction(dataService: DataService): DBIO[User] = {
    dataService.users.ensureUserForAction(loginInfo, teamId)
  }

  def ensureUser(dataService: DataService): Future[User] = {
    dataService.run(ensureUserAction(dataService))
  }

  def userInfoAction(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): DBIO[UserInfo] = {
    UserInfo.buildForAction(this, teamId, ws, dataService)
  }

  def messageInfo(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[MessageInfo] = {
    MessageInfo.buildFor(this, ws, dataService)
  }

  def detailsFor(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[JsObject]

  def recentMessages(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Seq[String]] = Future.successful(Seq())

  def navLinks(noSkills: Boolean, lambdaService: AWSLambdaService): String = {
    lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val skillsListPath = controllers.routes.ApplicationController.index(Some(teamId))
      val schedulingPath = controllers.routes.ScheduledActionsController.index(None, None, Some(teamId))
      if (noSkills) {
        s"""[Get started by teaching me something]($baseUrl$skillsListPath)"""
      } else {
        s"""[View all skills]($baseUrl$skillsListPath) · [Scheduling]($baseUrl$schedulingPath)"""
      }
    }.getOrElse("")
  }

  def teachMeLinkFor(lambdaService: AWSLambdaService): String = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.BehaviorEditorController.newGroup(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[teach me something new]($newBehaviorLink)"
  }

  def installLinkFor(lambdaService: AWSLambdaService): String = {
    val installLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.index(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[install new skills]($installLink)"
  }

  def iDontKnowHowToRespondMessageFor(lambdaService: AWSLambdaService)(implicit ec: ExecutionContext): String = {
    s"""
       |I don’t know how to respond to:
       |
       |> $messageText
       |
       |Type `${botPrefix}help` to see what I can do or ${teachMeLinkFor(lambdaService)}
    """.stripMargin
  }

  def noExactMatchResult(dataService: DataService, lambdaService: AWSLambdaService)(implicit actorSystem: ActorSystem): Future[BotResult] = {
    DisplayHelpBehavior(
      Some(messageText),
      None,
      Some(0),
      includeNameAndDescription = true,
      includeNonMatchingResults = false,
      isFirstTrigger = true,
      this,
      lambdaService,
      dataService
    ).result
  }

  def eventualMaybeDMChannel(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[String]]

  def maybeChannelToUseFor(behaviorVersion: BehaviorVersion, dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    eventualMaybeDMChannel(dataService).map { maybeDMChannel =>
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
                   maybeActions: Option[MessageActions] = None,
                   dataService: DataService
                 )(implicit actorSystem: ActorSystem): Future[Option[String]]

  def botPrefix: String = ""

  val invocationLogText: String

  def unformatTextFragment(text: String): String = {
    // Override for client-specific code to strip formatting from text
    text
  }

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               cache: CacheApi,
                               ws: WSClient,
                               configuration: Configuration,
                               actorSystem: ActorSystem
                             ): Future[Seq[BehaviorResponse]]

}
