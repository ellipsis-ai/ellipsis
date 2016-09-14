package models.bots.events

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.bots.UserInfo
import models.bots.conversations.conversation.Conversation
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait MessageContext extends Context {
  val fullMessageText: String

  def relevantMessageText: String = MessageContext.ellipsisRegex.replaceFirstIn(fullMessageText, "")

  val includesBotMention: Boolean

  def sendMessage(text: String, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit

  def teachMeLinkFor(lambdaService: AWSLambdaService): String = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.BehaviorEditorController.newBehavior(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[teach me something new]($newBehaviorLink)"
  }

  def installLinkFor(lambdaService: AWSLambdaService): String = {
    val installLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.installBehaviors(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[install new behaviors]($installLink)"
  }

  def iDontKnowHowToRespondMessageFor(lambdaService: AWSLambdaService)(implicit ec: ExecutionContext): String = {
    s"""
                   |I don't know how to respond to `$fullMessageText`
                   |
                   |Type `@ellipsis: help` to see what I can do or ${teachMeLinkFor(lambdaService)}
    """.stripMargin
  }

  def recentMessages(dataService: DataService): Future[Seq[String]] = Future.successful(Seq())
  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]]

  val name: String
  def userIdForContext: String
  val teamId: String
  val isResponseExpected: Boolean

  def userInfo(ws: WSClient, dataService: DataService): Future[UserInfo] = {
    UserInfo.forLoginInfo(LoginInfo(name, userIdForContext), teamId, ws, dataService)
  }

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    dataService.users.ensureUserFor(loginInfo, teamId)
  }

}

object MessageContext {

  def ellipsisRegex: Regex = """^(\.\.\.|…)""".r
}
