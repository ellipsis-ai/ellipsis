package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduling.Scheduled
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
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
  val maybeScheduled: Option[Scheduled] = None
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
      val path = controllers.routes.BehaviorEditorController.newGroup(Some(teamId))
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
      user <- ensureUser(dataService)
      groups <- maybeTeam.map { team =>
        dataService.behaviorGroups.allFor(team)
      }.getOrElse(Future.successful(Seq()))
      groupsData <- Future.sequence(groups.map { ea =>
        BehaviorGroupData.maybeFor(ea.id, user, None, dataService)
      }).map(_.flatten)
    } yield {
      val results = FuzzyMatcher(messageText, groupsData).run.filter { ea =>
        ea.patterns.exists {
          case p: MessageTrigger => true
          case _ => false
        }
      }
      val message = if (results.isEmpty) {
        iDontKnowHowToRespondMessageFor(lambdaService)
      } else {
        s"""Did you mean:
           |
           |${results.map(_.item.name).mkString("  \n")}
           |
           |Otherwise, try `${botPrefix}help` to see what else I can do or ${teachMeLinkFor(lambdaService)}
         """.stripMargin
      }
      SimpleTextResult(this, message, forcePrivateResponse = false)
    }
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

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]]

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None,
                   dataService: DataService
                 )(implicit actorSystem: ActorSystem): Future[Option[String]]

  // TODO: Remove this method if we're sure we don't want to use it in help anymore
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
                               configuration: Configuration
                             ): Future[Seq[BehaviorResponse]]

}
