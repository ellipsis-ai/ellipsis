package services.slack

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.{BotResult, MessageInfo, SimpleTextResult, UserInfo}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.triggers.TriggerFuzzyMatcher
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait MessageEvent {
  val name: String
  val userIdForContext: String
  val teamId: String
  val fullMessageText: String
  val includesBotMention: Boolean
  val maybeChannel: Option[String]

  def relevantMessageText: String = MessageEvent.ellipsisRegex.replaceFirstIn(fullMessageText, "")

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
       |I don't know how to respond to `$fullMessageText`
       |
                   |Type `@ellipsis: help` to see what I can do or ${teachMeLinkFor(lambdaService)}
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
        TriggerFuzzyMatcher(fullMessageText, triggers).
          run.
          map { case(trigger, _) => s"`${trigger.pattern}`" }
      val message = if (similarTriggers.isEmpty) {
        iDontKnowHowToRespondMessageFor(lambdaService)
      } else {
        s"""Did you mean:
           |
           |${similarTriggers.mkString("  \n")}
           |
         """.stripMargin
      }
      SimpleTextResult(message, forcePrivateResponse = false)
    }
  }

  def recentMessages(dataService: DataService): Future[Seq[String]] = Future.successful(Seq())

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation]
                 )(implicit ec: ExecutionContext): Future[Unit]

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    dataService.users.ensureUserFor(loginInfo, teamId)
  }

  def userInfo(ws: WSClient, dataService: DataService): Future[UserInfo] = {
    UserInfo.buildFor(this, teamId, ws, dataService)
  }

  def messageInfo(ws: WSClient, dataService: DataService): Future[MessageInfo] = {
    MessageInfo.buildFor(this, ws, dataService)
  }

  def detailsFor(ws: WSClient, dataService: DataService): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }

  val isResponseExpected: Boolean
  def isDirectMessage(channel: String): Boolean

  val conversationContext = conversationContextForChannel(maybeChannel.getOrElse(""))
  def conversationContextForChannel(channel: String) = name ++ "#" ++ channel

  def eventualMaybeDMChannel: Future[Option[String]]

  def conversationContextFor(behaviorVersion: BehaviorVersion): Future[String] = {
    eventualMaybeDMChannel.map { maybeDMChannel =>
      val maybeChannelToUse = if (behaviorVersion.forcePrivateResponse) {
        maybeDMChannel
      } else {
        maybeChannel
      }
      conversationContextForChannel(maybeChannelToUse.getOrElse(""))
    }
  }

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    dataService.conversations.allOngoingFor(userIdForContext, conversationContext, maybeChannel.exists(isDirectMessage))
  }

  def unformatTextFragment(text: String): String = {
    // Override for client-specific code to strip formatting from text
    text
  }

}

object MessageEvent {

  def ellipsisRegex: Regex = """^(\.\.\.|…)""".r
}
