package models.bots

import models.IDs
import models.accounts.OAuth2Application
import models.bots.config.RequiredOAuth2ApiConfig
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

  def sendIn(context: MessageContext): Unit = {
    context.sendMessage(fullText)
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

  override def sendIn(context: MessageContext): Unit = {
    // do nothing
  }

}

case class UnhandledErrorResult(maybeLogResult: Option[AWSLambdaLogResult]) extends BehaviorResultWithLogResult {

  val resultType = ResultType.UnhandledError

  def text: String = {
    val prompt = s"\nWe hit an error before calling $ON_SUCCESS_PARAM or $ON_ERROR_PARAM"
    Array(Some(prompt), maybeLogResult.flatMap(_.maybeTranslated)).flatten.mkString(":\n\n")
  }

}

case class HandledErrorResult(json: JsValue, maybeLogResult: Option[AWSLambdaLogResult]) extends BehaviorResultWithLogResult {

  val resultType = ResultType.HandledError

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  def text: String = {
    val maybeDetail = (json \ "errorMessage").toOption.map(processedResultFor)
    maybeDetail.getOrElse(s"$ON_ERROR_PARAM triggered")
  }
}

case class SyntaxErrorResult(json: JsValue, maybeLogResult: Option[AWSLambdaLogResult]) extends BehaviorResultWithLogResult {

  val resultType = ResultType.SyntaxError

  def text: String = {
    s"""
       |There's a syntax error in your function:
       |
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
        |${maybeLogResult.flatMap(_.maybeTranslated).getOrElse("")}
     """.stripMargin
  }
}

class NoCallbackTriggeredResult extends BehaviorResult {

  val resultType = ResultType.NoCallbackTriggered

  def text = s"It looks like neither callback was triggered — you need to make sure that `$ON_SUCCESS_PARAM`" ++
    s"is called to end every successful invocation and `$ON_ERROR_PARAM` is called to end every unsuccessful one"

}

case class MissingEnvVarsResult(missingEnvVars: Seq[String]) extends BehaviorResult {

  val resultType = ResultType.MissingEnvVar

  def text = {
    s"""
       |To use this behavior, you need the following environment variables defined:
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
                               cache: CacheApi,
                               configuration: Configuration
                               ) extends BehaviorResult {

  val key = IDs.next

  val resultType = ResultType.OAuth2TokenMissing

  def authLink: String = {
    configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.APIAccessController.linkCustomOAuth2Service(oAuth2Application.id, None, None, Some(key))
      s"$baseUrl$path"
    }.getOrElse("")
  }

  def text: String = {
    s"""To use this behavior, you need to [authenticate with ${oAuth2Application.name}]($authLink).
       |
       |You only need to do this one time for ${oAuth2Application.name}. You may be prompted to sign in to Ellipsis using your Slack account.
       |""".stripMargin
  }

  override def sendIn(context: MessageContext): Unit = {
    cache.set(key, event, 5.minutes)
    super.sendIn(context)
  }
}

case class RequiredApiNotReady(
                                required: RequiredOAuth2ApiConfig,
                                event: MessageEvent,
                                cache: CacheApi,
                                configuration: Configuration
                             ) extends BehaviorResult {

  val resultType = ResultType.RequiredApiNotReady

  def maybeConfigLink: Option[String] = required.behaviorVersion.editLinkFor(configuration)
  def configText: String = {
    maybeConfigLink.map { configLink =>
      s"You first must [configure the ${required.api.name} API]($configLink)"
    }.getOrElse(s"You first must configure the ${required.api.name} API")
  }

  def text: String = {
    s"This behavior is not ready to use. $configText."
  }

  override def sendIn(context: MessageContext): Unit = {
    super.sendIn(context)
  }
}
