package models.behaviors

import akka.actor.ActorSystem
import json.Formatting._
import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.oauth2application.OAuth2Application
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.templates.TemplateApplier
import play.api.Configuration
import play.api.libs.json._
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, CacheService, DataService}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ResultType extends Enumeration {
  type ResultType = Value
  val Success, SimpleText, TextWithActions, ConversationPrompt, NoResponse, ExecutionError, SyntaxError, NoCallbackTriggered, MissingTeamEnvVar, AWSDown, OAuth2TokenMissing, RequiredApiNotReady = Value
}

sealed trait BotResult {
  val resultType: ResultType.Value
  val forcePrivateResponse: Boolean
  val event: Event
  val maybeConversation: Option[Conversation]
  def files: Seq[UploadFileSpec] = Seq()
  val shouldInterrupt: Boolean = true
  def text: String
  def fullText: String = text
  def hasText: Boolean = fullText.trim.nonEmpty

  def maybeChannelForSendAction(maybeConversation: Option[Conversation], dataService: DataService)(implicit actorSystem: ActorSystem): DBIO[Option[String]] = {
    event.maybeChannelForSendAction(forcePrivateResponse, maybeConversation, dataService)
  }

  def maybeChannelForSend(maybeConversation: Option[Conversation], dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    dataService.run(maybeChannelForSendAction(maybeConversation, dataService))
  }

  def maybeOngoingConversation(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[Conversation]] = {
    maybeChannelForSend(None, dataService).flatMap { maybeChannel =>
      dataService.conversations.findOngoingFor(event.userIdForContext, event.context, maybeChannel, event.maybeThreadId)
    }
  }

  val interruptionPrompt = {
    val action = if (maybeConversation.isDefined) { "ask" } else { "tell" }
    s"You haven't answered my question yet, but I have something new to $action you."
  }

  def interruptOngoingConversationsForAction(dataService: DataService)(implicit actorSystem: ActorSystem): DBIO[Boolean] = {
    if (maybeConversation.exists(_.maybeThreadId.isDefined)) {
      DBIO.successful(false)
    } else {
      maybeChannelForSendAction(maybeConversation, dataService).flatMap { maybeChannelForSend =>
        dataService.conversations.allOngoingForAction(event.userIdForContext, event.context, maybeChannelForSend, event.maybeThreadId).flatMap { ongoing =>
          val toInterrupt = ongoing.filterNot(ea => maybeConversation.map(_.id).contains(ea.id))
          DBIO.sequence(toInterrupt.map { ea =>
            dataService.conversations.backgroundAction(ea, interruptionPrompt, includeUsername = true)
          })
        }
      }.map(interruptionResults => interruptionResults.nonEmpty)
    }
  }

  def beforeSend(): Unit = {}

  val shouldSend: Boolean = true

  def maybeActions: Option[MessageActions] = None
}

trait BotResultWithLogResult extends BotResult {
  val maybeLogResult: Option[AWSLambdaLogResult]

  val maybeAuthorLog: Option[String] = {
    maybeLogResult.map(_.authorDefinedLogStatements).filter(_.nonEmpty)
  }

  val maybeAuthorLogFile: Option[UploadFileSpec] = {
    maybeAuthorLog.map { log =>
      UploadFileSpec(Some(log), Some("text"), Some("Developer log"))
    }
  }

  override def files: Seq[UploadFileSpec] = {
    super.files ++ Seq(maybeAuthorLogFile).flatten
  }
}

case class InvalidFilesException(message: String) extends Exception {
  def responseText: String =
    s"""Invalid files passed to `ellipsis.success()`
       |
       |Errors: $message
       |
       |The value for the `files` property should be an array like:
       |
       |```
       |[
       |  {
       |    content: "The content…",
       |    filetype: "text",
       |    filename: "filname.txt"
       |  }, ...
       |]
       |```
     """.stripMargin
}

case class SuccessResult(
                          event: Event,
                          maybeConversation: Option[Conversation],
                          result: JsValue,
                          resultWithOptions: JsValue,
                          parametersWithValues: Seq[ParameterWithValue],
                          maybeResponseTemplate: Option[String],
                          maybeLogResult: Option[AWSLambdaLogResult],
                          forcePrivateResponse: Boolean
                          ) extends BotResultWithLogResult {

  val resultType = ResultType.Success

  override def files: Seq[UploadFileSpec] = {
    val authoredFiles = (resultWithOptions \ "files").validateOpt[Seq[UploadFileSpec]] match {
      case JsSuccess(maybeFiles, _) => maybeFiles.getOrElse(Seq())
      case JsError(errs) => throw InvalidFilesException(errs.map { case (_, validationErrors) =>
        validationErrors.map(_.message).mkString(", ")
      }.mkString(", "))
    }
    authoredFiles ++ super.files
  }

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

  override val shouldSend: Boolean = false
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

case class ExecutionErrorResult(
                                 event: Event,
                                 maybeConversation: Option[Conversation],
                                 behaviorVersion: BehaviorVersion,
                                 dataService: DataService,
                                 configuration: Configuration,
                                 json: JsValue,
                                 maybeLogResult: Option[AWSLambdaLogResult]
                               ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.ExecutionError
  val functionLines = behaviorVersion.functionBody.split("\n").length
  val howToIncludeStackTraceMessage = "\n\nTo include a stack trace, throw an `Error` object in your code.  \ne.g. `throw new Error(\"Something went wrong.\")`"

  def text: String = {
    s"I encountered an error in ${linkToBehaviorFor("one of your skills")}" +
      maybeLogResult.flatMap(_.maybeUserErrorMessage).map { userError =>
        s":\n\n$userError"
      }.getOrElse(".")
  }

  private def logContainsStackTrace(log: String): Boolean = {
    val lines = log.lines.toList
    lines.length > 1 && lines.tail.exists(_.matches("""^\s*at .+?\(.+?:\d+:\d+\)"""))
  }

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  private val maybeCallbackErrorMessage: Option[String] = {
    (json \ "errorMessage").toOption.map(processedResultFor).filterNot {
      _.matches("""RequestId: \S+ Process exited before completing request""")
    }
  }

  private val maybeThrownLogMessage: Option[String] = {
    maybeLogResult.flatMap(_.maybeTranslated(functionLines)).map { logText =>
      if (!logContainsStackTrace(logText)) {
        logText + howToIncludeStackTraceMessage
      } else {
        logText
      }
    }
  }

  private def maybeErrorLog: Option[String] = {
    val result = Seq(maybeCallbackErrorMessage, maybeThrownLogMessage).flatten.mkString("\n")
    Option(result).filter(_.nonEmpty)
  }

  override def files: Seq[UploadFileSpec] = {
    val log = maybeAuthorLog.map(_ + "\n").getOrElse("") + maybeErrorLog.getOrElse("")
    if (log.nonEmpty) {
      Seq(UploadFileSpec(Some(log), Some("text"), Some("Developer log")))
    } else {
      Seq()
    }
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
       |````
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
       |````
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
                                 missingEnvVars: Set[String],
                                 botPrefix: String
                               ) extends BotResult with WithBehaviorLink {

  val resultType = ResultType.MissingTeamEnvVar

  def text = {
    s"""
       |To use ${linkToBehaviorFor("this skill")}, you need the following environment variables defined:
       |${missingEnvVars.map( ea => s"\n- $ea").mkString("")}
       |
       |You can define an environment variable by typing something like:
       |
       |`${botPrefix}set env ENV_VAR_NAME value`
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
                               cacheService: CacheService,
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

  override def beforeSend: Unit = cacheService.cacheEvent(key, event, 5.minutes)
}

case class RequiredApiNotReady(
                                required: RequiredOAuth2ApiConfig,
                                event: Event,
                                maybeConversation: Option[Conversation],
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
