package models.behaviors

import akka.actor.ActorSystem
import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.oauth2application.OAuth2Application
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.templates.TemplateApplier
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.{JsDefined, JsValue}
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ResultType extends Enumeration {
  type ResultType = Value
  val Success, SimpleText, TextWithActions, ConversationPrompt, NoResponse, UnhandledError, HandledError, SyntaxError, NoCallbackTriggered, MissingTeamEnvVar, AWSDown, OAuth2TokenMissing, RequiredApiNotReady = Value
}

sealed trait BotResult {
  val resultType: ResultType.Value
  val forcePrivateResponse: Boolean
  val event: Event
  val maybeConversation: Option[Conversation]
  val shouldInterrupt: Boolean = true
  def text: String
  def fullText: String = text
  def hasText: Boolean = fullText.trim.nonEmpty

  def maybeChannelForSend(maybeConversation: Option[Conversation], dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    event.maybeChannelForSend(forcePrivateResponse, maybeConversation, dataService)
  }

  def maybeOngoingConversation(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[Conversation]] = {
    maybeChannelForSend(None, dataService).flatMap { maybeChannel =>
      dataService.conversations.findOngoingFor(event.userIdForContext, event.context, maybeChannel, event.maybeThreadId)
    }
  }

  val interruptionPrompt = "You haven't answered my question yet, but I have something new to ask you."

  def interruptOngoingConversationsFor(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Boolean] = {
    if (maybeConversation.exists(_.maybeThreadId.isDefined)) {
      Future.successful(false)
    } else {
      maybeChannelForSend(maybeConversation, dataService).flatMap { maybeChannelForSend =>
        dataService.conversations.allOngoingFor(event.userIdForContext, event.context, maybeChannelForSend, event.maybeThreadId).flatMap { ongoing =>
          val toInterrupt = ongoing.filterNot(ea => maybeConversation.map(_.id).contains(ea.id))
          Future.sequence(toInterrupt.map { ea =>
            dataService.conversations.background(ea, interruptionPrompt, includeUsername = true)
          })
        }
      }.map(interruptionResults => interruptionResults.nonEmpty)
    }
  }

  def sendIn(
              maybeShouldUnfurl: Option[Boolean],
              dataService: DataService,
              maybeIntro: Option[String] = None,
              maybeInterruptionIntro: Option[String] = None
            )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    for {
      didInterrupt <- if (shouldInterrupt) {
        interruptOngoingConversationsFor(dataService)
      } else {
        Future.successful(false)
      }
      _ <- maybeIntro.map { intro =>
        val introToSend = if (didInterrupt) {
          maybeInterruptionIntro.getOrElse(intro)
        } else {
          intro
        }
        SimpleTextResult(event, maybeConversation, introToSend, forcePrivateResponse).sendIn(None, dataService)
      }.getOrElse {
        Future.successful({})
      }
      sendResult <- event.sendMessage(fullText, forcePrivateResponse, maybeShouldUnfurl, maybeConversation, maybeActions, dataService)
    } yield sendResult
  }

  def maybeActions: Option[MessageActions] = None
}

trait BotResultWithLogResult extends BotResult {
  val maybeLogResult: Option[AWSLambdaLogResult]
  val logStatements = maybeLogResult.map(_.userDefinedLogStatements).getOrElse("")

  override def fullText: String = logStatements ++ text

}

case class SuccessResult(
                          event: Event,
                          maybeConversation: Option[Conversation],
                          result: JsValue,
                          parametersWithValues: Seq[ParameterWithValue],
                          maybeResponseTemplate: Option[String],
                          maybeLogResult: Option[AWSLambdaLogResult],
                          forcePrivateResponse: Boolean
                          ) extends BotResultWithLogResult {

  val resultType = ResultType.Success

  def text: String = {
    val inputs = parametersWithValues.map { ea => (ea.parameter.name, ea.preparedValue) }
    TemplateApplier(maybeResponseTemplate, JsDefined(result), inputs).apply
  }
}

case class SimpleTextResult(event: Event, maybeConversation: Option[Conversation], simpleText: String, forcePrivateResponse: Boolean) extends BotResult {

  val resultType = ResultType.SimpleText

  def text: String = simpleText

}

case class TextWithActionsResult(event: Event, maybeConversation: Option[Conversation], simpleText: String, forcePrivateResponse: Boolean, actions: MessageActions) extends BotResult {
  val resultType = ResultType.TextWithActions

  def text: String = simpleText

  override def maybeActions: Option[MessageActions] = {
    Some(actions)
  }
}

case class NoResponseResult(event: Event, maybeConversation: Option[Conversation], maybeLogResult: Option[AWSLambdaLogResult]) extends BotResultWithLogResult {

  val resultType = ResultType.NoResponse
  val forcePrivateResponse = false // N/A
  override val shouldInterrupt = false

  def text: String = ""

  override def sendIn(
                       maybeShouldUnfurl: Option[Boolean],
                       dataService: DataService,
                       maybeIntro: Option[String] = None,
                       maybeInterruptionIntro: Option[String] = None
                     )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    // do nothing
    Future.successful(None)
  }

}

trait WithBehaviorLink {

  val behaviorVersion: BehaviorVersion
  val dataService: DataService
  val configuration: Configuration
  val forcePrivateResponse = behaviorVersion.forcePrivateResponse

  def link: String = dataService.behaviors.editLinkFor(behaviorVersion.group.id, Some(behaviorVersion.behavior.id), configuration)

  def linkToBehaviorFor(text: String): String = {
    s"[$text](${link})"
  }
}

case class UnhandledErrorResult(
                                 event: Event,
                                 maybeConversation: Option[Conversation],
                                 behaviorVersion: BehaviorVersion,
                                 dataService: DataService,
                                 configuration: Configuration,
                                 maybeLogResult: Option[AWSLambdaLogResult]
                               ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.UnhandledError

  def text: String = {
    val prompt = s"\nI encountered an error in ${linkToBehaviorFor("one of your skills")} before calling `$SUCCESS_CALLBACK `or `$ERROR_CALLBACK`"
    Array(Some(prompt), maybeLogResult.flatMap(_.maybeTranslated)).flatten.mkString(":\n\n")
  }

}

case class HandledErrorResult(
                               event: Event,
                               maybeConversation: Option[Conversation],
                               behaviorVersion: BehaviorVersion,
                               dataService: DataService,
                               configuration: Configuration,
                               json: JsValue,
                               maybeLogResult: Option[AWSLambdaLogResult]
                             ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.HandledError

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  def text: String = {
    val detail = (json \ "errorMessage").toOption.map(processedResultFor).map { msg =>
      s":\n\n```\n$msg\n```"
    }.getOrElse("")
    s"I encountered an error in ${linkToBehaviorFor("one of your skills")}$detail"
  }
}

case class SyntaxErrorResult(
                              event: Event,
                              maybeConversation: Option[Conversation],
                              behaviorVersion: BehaviorVersion,
                              dataService: DataService,
                              configuration: Configuration,
                              json: JsValue,
                              maybeLogResult: Option[AWSLambdaLogResult]
                            ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.SyntaxError

  def text: String = {
    s"""
       |There's a syntax error in your skill:
       |
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
       |
       |${linkToBehaviorFor("Take a look in the skill editor")} for more details.
     """.stripMargin
  }
}

case class NoCallbackTriggeredResult(
                                      event: Event,
                                      maybeConversation: Option[Conversation],
                                      behaviorVersion: BehaviorVersion,
                                      dataService: DataService,
                                      configuration: Configuration
                                    ) extends BotResult with WithBehaviorLink {

  val resultType = ResultType.NoCallbackTriggered

  def text = s"It looks like neither callback was triggered in ${linkToBehaviorFor("your skill")}— you need to make sure that `$SUCCESS_CALLBACK`" ++
    s"is called to end every successful invocation and `$ERROR_CALLBACK` is called to end every unsuccessful one"

}

case class MissingTeamEnvVarsResult(
                                 event: Event,
                                 maybeConversation: Option[Conversation],
                                 behaviorVersion: BehaviorVersion,
                                 dataService: DataService,
                                 configuration: Configuration,
                                 missingEnvVars: Seq[String]
                               ) extends BotResult with WithBehaviorLink {

  val resultType = ResultType.MissingTeamEnvVar

  def text = {
    s"""
       |To use ${linkToBehaviorFor("this skill")}, you need the following environment variables defined:
       |${missingEnvVars.map( ea => s"\n- $ea").mkString("")}
       |
       |You can define an environment variable by typing something like:
       |
       |`${event.botPrefix}set env ENV_VAR_NAME value`
    """.stripMargin
  }

}

case class AWSDownResult(event: Event, maybeConversation: Option[Conversation]) extends BotResult {

  val resultType = ResultType.AWSDown
  val forcePrivateResponse = false

  def text: String = {
    """
      |The Amazon Web Service that Ellipsis relies upon is currently down.
      |
      |Try asking Ellipsis anything later to check on the status.
      |""".stripMargin
  }

}

case class OAuth2TokenMissing(
                               oAuth2Application: OAuth2Application,
                               event: Event,
                               maybeConversation: Option[Conversation],
                               loginToken: LoginToken,
                               cache: CacheApi,
                               configuration: Configuration
                               ) extends BotResult {

  val key = IDs.next

  val resultType = ResultType.OAuth2TokenMissing

  val forcePrivateResponse = true

  def authLink: String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val redirectPath = controllers.routes.APIAccessController.linkCustomOAuth2Service(oAuth2Application.id, None, None, Some(key), None)
    val redirect = s"$baseUrl$redirectPath"
    val authPath = controllers.routes.SocialAuthController.loginWithToken(loginToken.value, Some(redirect))
    s"$baseUrl$authPath"
  }

  def text: String = {
    s"""To use this skill, you need to [authenticate with ${oAuth2Application.name}]($authLink).
       |
       |You only need to do this one time for ${oAuth2Application.name}. You may be prompted to sign in to Ellipsis using your Slack account.
       |""".stripMargin
  }

  override def sendIn(
                       maybeShouldUnfurl: Option[Boolean],
                       dataService: DataService,
                       maybeIntro: Option[String] = None,
                       maybeInterruptionIntro: Option[String] = None
                     )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    cache.set(key, event, 5.minutes)
    super.sendIn(maybeShouldUnfurl, dataService)
  }
}

case class RequiredApiNotReady(
                                required: RequiredOAuth2ApiConfig,
                                event: Event,
                                maybeConversation: Option[Conversation],
                                cache: CacheApi,
                                dataService: DataService,
                                configuration: Configuration
                             ) extends BotResult {

  val resultType = ResultType.RequiredApiNotReady
  val forcePrivateResponse = true

  def configLink: String = dataService.behaviors.editLinkFor(required.groupVersion.group.id, None, configuration)
  def configText: String = {
    s"You first must [configure the ${required.api.name} API]($configLink)"
  }

  def text: String = {
    s"This skill is not ready to use. $configText."
  }

}
