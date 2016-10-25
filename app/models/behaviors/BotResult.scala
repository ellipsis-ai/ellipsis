package models.behaviors

import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.oauth2application.OAuth2Application
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.events.{MessageContext, MessageEvent}
import models.behaviors.templates.TemplateApplier
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.{JsDefined, JsString, JsValue}
import services.AWSLambdaConstants._
import services.AWSLambdaLogResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ResultType extends Enumeration {
  type ResultType = Value
  val Success, ConversationPrompt, NoResponse, UnhandledError, HandledError, SyntaxError, NoCallbackTriggered, MissingEnvVar, AWSDown, OAuth2TokenMissing, RequiredApiNotReady = Value
}

sealed trait BotResult {
  val resultType: ResultType.Value
  val forcePrivateResponse: Boolean
  def text: String
  def fullText: String = text
  def hasText: Boolean = fullText.trim.nonEmpty

  def sendIn(context: MessageContext, maybeShouldUnfurl: Option[Boolean] = None): Unit = {
    context.sendMessage(fullText, forcePrivateResponse, maybeShouldUnfurl)
  }
}

trait BotResultWithLogResult extends BotResult {
  val maybeLogResult: Option[AWSLambdaLogResult]
  val logStatements = maybeLogResult.map(_.userDefinedLogStatements).getOrElse("")

  override def fullText: String = logStatements ++ text

}

case class SuccessResult(
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

case class SimpleTextResult(simpleText: String, forcePrivateResponse: Boolean) extends BotResult {

  val resultType = ResultType.ConversationPrompt

  def text: String = simpleText

}

case class NoResponseResult(maybeLogResult: Option[AWSLambdaLogResult]) extends BotResultWithLogResult {

  val resultType = ResultType.NoResponse
  val forcePrivateResponse = false // N/A

  def text: String = ""

  override def sendIn(context: MessageContext, maybeShouldUnfurl: Option[Boolean] = None): Unit = {
    // do nothing
  }

}

trait WithBehaviorLink {

  val behaviorVersion: BehaviorVersion
  val configuration: Configuration
  val forcePrivateResponse = behaviorVersion.forcePrivateResponse

  def link: String = behaviorVersion.editLinkFor(configuration)

  def linkToBehaviorFor(text: String): String = {
    s"[$text](${link})"
  }
}

case class UnhandledErrorResult(
                                 behaviorVersion: BehaviorVersion,
                                 configuration: Configuration,
                                 maybeLogResult: Option[AWSLambdaLogResult]
                               ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.UnhandledError

  def text: String = {
    val prompt = s"\nI encountered an error in ${linkToBehaviorFor("one of your behaviors")} before calling `$SUCCESS_CALLBACK `or `$ERROR_CALLBACK`"
    Array(Some(prompt), maybeLogResult.flatMap(_.maybeTranslated)).flatten.mkString(":\n\n")
  }

}

case class HandledErrorResult(
                               behaviorVersion: BehaviorVersion,
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
      s":\n\n```$msg```"
    }.getOrElse("")
    s"I encountered an error in ${linkToBehaviorFor("one of your behaviors")}$detail"
  }
}

case class SyntaxErrorResult(
                              behaviorVersion: BehaviorVersion,
                              configuration: Configuration,
                              json: JsValue,
                              maybeLogResult: Option[AWSLambdaLogResult]
                            ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.SyntaxError

  def text: String = {
    s"""
       |There's a syntax error in your behavior:
       |
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
       |
       |${linkToBehaviorFor("Take a look in the behavior editor")} for more details.
     """.stripMargin
  }
}

case class NoCallbackTriggeredResult(
                                      behaviorVersion: BehaviorVersion,
                                      configuration: Configuration
                                    ) extends BotResult with WithBehaviorLink {

  val resultType = ResultType.NoCallbackTriggered

  def text = s"It looks like neither callback was triggered in ${linkToBehaviorFor("your behavior")}— you need to make sure that `$SUCCESS_CALLBACK`" ++
    s"is called to end every successful invocation and `$ERROR_CALLBACK` is called to end every unsuccessful one"

}

case class MissingEnvVarsResult(
                                 behaviorVersion: BehaviorVersion,
                                 configuration: Configuration,
                                 missingEnvVars: Seq[String]
                               ) extends BotResult with WithBehaviorLink {

  val resultType = ResultType.MissingEnvVar

  def text = {
    s"""
       |To use ${linkToBehaviorFor("this behavior")}, you need the following environment variables defined:
       |${missingEnvVars.map( ea => s"\n- $ea").mkString("")}
        |
        |You can define an environment variable by typing something like:
        |
        |`@ellipsis: set env ENV_VAR_NAME value`
    """.stripMargin
  }

}

class AWSDownResult extends BotResult {

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
                               event: MessageEvent,
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
    s"""To use this behavior, you need to [authenticate with ${oAuth2Application.name}]($authLink).
       |
       |You only need to do this one time for ${oAuth2Application.name}. You may be prompted to sign in to Ellipsis using your Slack account.
       |""".stripMargin
  }

  override def sendIn(context: MessageContext, maybeShouldUnfurl: Option[Boolean] = None): Unit = {
    cache.set(key, event, 5.minutes)
    super.sendIn(context, maybeShouldUnfurl)
  }
}

case class RequiredApiNotReady(
                                required: RequiredOAuth2ApiConfig,
                                event: MessageEvent,
                                cache: CacheApi,
                                configuration: Configuration
                             ) extends BotResult {

  val resultType = ResultType.RequiredApiNotReady
  val forcePrivateResponse = true

  def configLink: String = required.behaviorVersion.editLinkFor(configuration)
  def configText: String = {
    s"You first must [configure the ${required.api.name} API]($configLink)"
  }

  def text: String = {
    s"This behavior is not ready to use. $configText."
  }

}
