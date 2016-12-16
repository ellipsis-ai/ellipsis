package models.behaviors.events

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.{MessageInfo, UserInfo}
import models.behaviors.conversations.conversation.Conversation
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait MessageContext extends Context {
  val fullMessageText: String

  def relevantMessageText: String = MessageContext.ellipsisRegex.replaceFirstIn(fullMessageText, "")

  val includesBotMention: Boolean

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

  def recentMessages(dataService: DataService): Future[Seq[String]] = Future.successful(Seq())
  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]]

  val name: String
  val maybeChannel: Option[String]
  val conversationContext: String = name
  def conversationContextFor(behaviorVersion: BehaviorVersion) = conversationContext
  def userIdForContext: String
  val teamId: String
  val isResponseExpected: Boolean

  def userInfo(ws: WSClient, dataService: DataService): Future[UserInfo] = {
    UserInfo.buildFor(this, teamId, ws, dataService)
  }

  def messageInfo(ws: WSClient, dataService: DataService): Future[MessageInfo] = {
    MessageInfo.buildFor(this, ws, dataService)
  }

  def detailsFor(ws: WSClient, dataService: DataService): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)

  def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    dataService.users.ensureUserFor(loginInfo, teamId)
  }

}

object MessageContext {

  def ellipsisRegex: Regex = """^(\.\.\.|…)""".r
}
