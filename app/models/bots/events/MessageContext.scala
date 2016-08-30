package models.bots.events

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.{LinkedAccount, User}
import models.bots.UserInfo
import models.bots.conversations.Conversation
import org.joda.time.DateTime
import play.api.libs.ws.WSClient
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

trait MessageContext extends Context {
  val fullMessageText: String

  def relevantMessageText: String = MessageContext.ellipsisRegex.replaceFirstIn(fullMessageText, "")

  val includesBotMention: Boolean

  def sendMessage(text: String, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit

  def teachMeLinkFor(lambdaService: AWSLambdaService): String = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.newBehavior(Some(teamId))
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

  def recentMessages: DBIO[Seq[String]] = DBIO.successful(Seq())
  def maybeOngoingConversation: DBIO[Option[Conversation]]

  val name: String
  def userIdForContext: String
  val teamId: String
  val isResponseExpected: Boolean

  def userInfo(ws: WSClient): DBIO[UserInfo] = UserInfo.forLoginInfo(LoginInfo(name, userIdForContext), teamId, ws)

  def ensureUser(implicit ec: ExecutionContext): DBIO[User] = {
    val loginInfo = LoginInfo(name, userIdForContext)
    LinkedAccount.find(loginInfo, teamId).flatMap { maybeLinkedAccount =>
      maybeLinkedAccount.map { linkedAccount =>
        DBIO.successful(linkedAccount.user)
      }.getOrElse {
        User.createOnTeamWithId(teamId).save.flatMap { user =>
          LinkedAccount(user, loginInfo, DateTime.now).save.map(_.user)
        }
      }
    }
  }

}

object MessageContext {

  def ellipsisRegex: Regex = """^(\.\.\.|…)""".r
}
