package models.bots

import models.bots.templates.TemplateApplier
import play.api.libs.json.{JsDefined, JsString, JsValue}
import services.AWSLambdaConstants._
import services.AWSLambdaLogResult
import scala.concurrent.ExecutionContext.Implicits.global

object ResultType extends Enumeration {
  type ResultType = Value
  val Success, UnhandledError, HandledError, SyntaxError, NoCallbackTriggered, MissingEnvVar, AWSDown = Value
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
  val logResult: AWSLambdaLogResult

  override def fullText: String = logResult.userDefinedLogStatements ++ text

}

case class SuccessResult(
                          result: JsValue,
                          parametersWithValues: Seq[ParameterWithValue],
                          maybeResponseTemplate: Option[String],
                          logResult: AWSLambdaLogResult
                          ) extends BehaviorResultWithLogResult {

  val resultType = ResultType.Success

  def text: String = {
    val inputs = parametersWithValues.map { ea => (ea.parameter.name, JsString(ea.value)) }
    TemplateApplier(maybeResponseTemplate, JsDefined(result), inputs).apply
  }
}

case class UnhandledErrorResult(logResult: AWSLambdaLogResult) extends BehaviorResultWithLogResult {

  val resultType = ResultType.UnhandledError

  def text: String = {
    val prompt = s"\nWe hit an error before calling $ON_SUCCESS_PARAM or $ON_ERROR_PARAM"
    Array(Some(prompt), logResult.maybeTranslated).flatten.mkString(":\n\n")
  }

}

case class HandledErrorResult(json: JsValue, logResult: AWSLambdaLogResult) extends BehaviorResultWithLogResult {

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

case class SyntaxErrorResult(json: JsValue, logResult: AWSLambdaLogResult) extends BehaviorResultWithLogResult {

  val resultType = ResultType.SyntaxError

  def text: String = {
    s"""
       |There's a syntax error in your function:
       |
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
        |${logResult.maybeTranslated.getOrElse("")}
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
