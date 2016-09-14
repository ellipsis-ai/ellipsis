package models.bots

import models.IDs
import models.accounts.logintoken.LoginToken
import models.accounts.oauth2application.OAuth2Application
import models.bots.behaviorversion.BehaviorVersion
import models.bots.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.bots.events.{MessageContext, MessageEvent}
import models.bots.templates.TemplateApplier
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

sealed trait BehaviorResult {
  val resultType: ResultType.Value
  def text: String
  def fullText: String = text
  def hasText: Boolean = fullText.trim.nonEmpty

  def sendIn(context: MessageContext, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None): Unit = {
    context.sendMessage(fullText, forcePrivate, maybeShouldUnfurl)
  }
}

trait BehaviorResultWithLogResult extends BehaviorResult {
  val maybeLogResult: Option[AWSLambdaLogResult]
  val logStatements = maybeLogResult.map(_.userDefinedLogStatements).getOrElse("")

  override def fullText: String = logStatements ++ text

}

case class SuccessResult(
                          result: JsValue,
                          parametersWithValues: Seq[ParameterWithValue],
                          maybeResponseTemplate: Option[String],
                          maybeLogResult: Option[AWSLambdaLogResult]
                          ) extends BehaviorResultWithLogResult {

  val resultType = ResultType.Success

  def text: String = {
    val inputs = parametersWithValues.map { ea => (ea.parameter.name, JsString(ea.value)) }
    TemplateApplier(maybeResponseTemplate, JsDefined(result), inputs).apply
  }
}

case class SimpleTextResult(simpleText: String) extends BehaviorResult {

  val resultType = ResultType.ConversationPrompt

  def text: String = simpleText

}

case class NoResponseResult(maybeLogResult: Option[AWSLambdaLogResult]) extends BehaviorResultWithLogResult {

  val resultType = ResultType.NoResponse

  def text: String = ""

  override def sendIn(context: MessageContext, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None): Unit = {
    // do nothing
  }

}

trait WithBehaviorLink {

  val behaviorVersion: BehaviorVersion
  val configuration: Configuration

  def link: String = behaviorVersion.editLinkFor(configuration)

  def linkToBehaviorFor(text: String): String = {
    s"[$text](${link})"
  }
}

case class UnhandledErrorResult(
                                 behaviorVersion: BehaviorVersion,
                                 configuration: Configuration,
                                 maybeLogResult: Option[AWSLambdaLogResult]
                               ) extends BehaviorResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.UnhandledError

  def text: String = {
    val prompt = s"\nWe hit an error in ${linkToBehaviorFor("one of your behaviors")} before calling `$SUCCESS_CALLBACK `or `$ERROR_CALLBACK`"
    Array(Some(prompt), maybeLogResult.flatMap(_.maybeTranslated)).flatten.mkString(":\n\n")
  }

}

case class HandledErrorResult(
                               behaviorVersion: BehaviorVersion,
                               configuration: Configuration,
                               json: JsValue,
                               maybeLogResult: Option[AWSLambdaLogResult]
                             ) extends BehaviorResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.HandledError

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  def text: String = {
    val detail = (json \ "errorMessage").toOption.map(processedResultFor).map { msg =>
      s":\n\n$msg"
    }.getOrElse("")
    s"$ERROR_CALLBACK triggered in ${linkToBehaviorFor("one of your behaviors")}$detail"
  }
}

case class SyntaxErrorResult(
                              behaviorVersion: BehaviorVersion,
                              configuration: Configuration,
                              json: JsValue,
                              maybeLogResult: Option[AWSLambdaLogResult]
                            ) extends BehaviorResultWithLogResult with WithBehaviorLink {

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
                                    ) extends BehaviorResult with WithBehaviorLink {

  val resultType = ResultType.NoCallbackTriggered

  def text = s"It looks like neither callback was triggered in ${linkToBehaviorFor("your behavior")}— you need to make sure that `$SUCCESS_CALLBACK`" ++
    s"is called to end every successful invocation and `$ERROR_CALLBACK` is called to end every unsuccessful one"

}

case class MissingEnvVarsResult(
                                 behaviorVersion: BehaviorVersion,
                                 configuration: Configuration,
                                 missingEnvVars: Seq[String]
                               ) extends BehaviorResult with WithBehaviorLink {

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

class AWSDownResult extends BehaviorResult {

  val resultType = ResultType.AWSDown

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
                               ) extends BehaviorResult {

  val key = IDs.next

  val resultType = ResultType.OAuth2TokenMissing

  def authLink: String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val redirectPath = controllers.routes.APIAccessController.linkCustomOAuth2Service(oAuth2Application.id, None, None, Some(key))
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

  override def sendIn(context: MessageContext, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None): Unit = {
    cache.set(key, event, 5.minutes)
    super.sendIn(context, forcePrivate = true, Some(false))
  }
}

case class RequiredApiNotReady(
                                required: RequiredOAuth2ApiConfig,
                                event: MessageEvent,
                                cache: CacheApi,
                                configuration: Configuration
                             ) extends BehaviorResult {

  val resultType = ResultType.RequiredApiNotReady

  def configLink: String = required.behaviorVersion.editLinkFor(configuration)
  def configText: String = {
    s"You first must [configure the ${required.api.name} API]($configLink)"
  }

  def text: String = {
    s"This behavior is not ready to use. $configText."
  }

}
