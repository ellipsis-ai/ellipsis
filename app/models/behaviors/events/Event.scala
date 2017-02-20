package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.behaviors.{BotResult, MessageInfo, SimpleTextResult, UserInfo}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduledmessage.ScheduledMessage
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}
import utils.FuzzyMatcher

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
  val maybeScheduledMessage: Option[ScheduledMessage] = None
  val context = name
  val isResponseExpected: Boolean
  val includesBotMention: Boolean

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUser(dataService: DataService): Future[User] = {
    dataService.users.ensureUserFor(loginInfo, teamId)
  }

  def userInfo(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[UserInfo] = {
    UserInfo.buildFor(this, teamId, ws, dataService)
  }

  def messageInfo(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[MessageInfo] = {
    MessageInfo.buildFor(this, ws, dataService)
  }

  def detailsFor(ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }

  def recentMessages(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Seq[String]] = Future.successful(Seq())

  def skillListLinkFor(lambdaService: AWSLambdaService): String = {
    val skillListLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.index(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[View all skills]($skillListLink)"
  }

  def teachMeLinkFor(lambdaService: AWSLambdaService): String = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.BehaviorEditorController.newForNormalBehavior(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[teach me something new]($newBehaviorLink)"
  }

  def installLinkFor(lambdaService: AWSLambdaService): String = {
    val installLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.installBehaviors(Some(teamId))
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

  def noExactMatchResult(dataService: DataService, lambdaService: AWSLambdaService): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(teamId)
      triggers <- maybeTeam.map { team =>
        dataService.messageTriggers.allActiveFor(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val similarTriggers =
        FuzzyMatcher(messageText, triggers).
          run.
          map { case(trigger, _) => s"`${trigger.pattern}`" }
      val message = if (similarTriggers.isEmpty) {
        iDontKnowHowToRespondMessageFor(lambdaService)
      } else {
        s"""Did you mean:
           |
           |${similarTriggers.mkString("  \n")}
           |
           |Otherwise, try `${botPrefix}help` to see what else I can do or ${teachMeLinkFor(lambdaService)}
         """.stripMargin
      }
      SimpleTextResult(this, message, forcePrivateResponse = false)
    }
  }


  def eventualMaybeDMChannel(implicit actorSystem: ActorSystem): Future[Option[String]]

  def maybeChannelToUseFor(behaviorVersion: BehaviorVersion)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    eventualMaybeDMChannel.map { maybeDMChannel =>
      if (behaviorVersion.forcePrivateResponse) {
        maybeDMChannel
      } else {
        maybeChannel
      }
    }
  }

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]]

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None
                 )(implicit actorSystem: ActorSystem): Future[Option[String]]

  // TODO: Remove this method if we're sure we don't want to use it in help anymore
  def botPrefix: String = ""

  val invocationLogText: String

  def unformatTextFragment(text: String): String = {
    // Override for client-specific code to strip formatting from text
    text
  }

}
