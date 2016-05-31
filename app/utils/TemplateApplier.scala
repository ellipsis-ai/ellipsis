package util

import play.api.libs.json._
import services.AWSLambdaConstants._

import scala.util.matching.Regex

case class TemplateApplier(
                            maybeResponseTemplate: Option[String],
                            result: JsLookupResult,
                            inputs: Seq[(String, String)] = Seq()
                            ) {


  private def printJsValue(value: JsValue): String = {
    value match {
      case s: JsString => s.value
      case _ => value.toString
    }
  }

  private def lookUp(obj: JsLookupResult, properties: Array[String]): Option[String] = {
    properties.headOption.map { property =>
      lookUp(obj \ property, properties.tail)
    }.getOrElse {
      obj.toOption.map(printJsValue)
    }
  }

  private def pathReplacement(result: JsLookupResult): Regex.Match => String = {
    m: Regex.Match =>
      m.subgroups.headOption.map { path =>
        if (path == null) {
          result.toOption.map(printJsValue).getOrElse("not found")
        } else {
          val segments = path.split("\\.").filter(_.nonEmpty)
          lookUp(result, segments).getOrElse(s"$RESULT_KEY$path not found")
        }
      }.getOrElse(m.toString())
  }

  def applyInputsTo(responseTemplate: String, remainingInputs: Seq[(String, String)]): String = {
    remainingInputs.headOption.map { case(paramName, value) =>
      val regex = s"""(?s)(\\{\\s*$paramName\\s*\\})""".r
      val applied = regex.replaceAllIn(responseTemplate, value)
      applyInputsTo(applied, remainingInputs.tail)
    }.getOrElse(responseTemplate)
  }

  def applyResultTo(responseTemplate: String): String = {
    val pathsRegex = s"""(?s)\\{$RESULT_KEY(\\.\\S+)?\\}""".r
    pathsRegex.replaceAllIn(responseTemplate, pathReplacement(result))
  }

  def apply: String = {
    maybeResponseTemplate.map { responseTemplate =>
      applyInputsTo(applyResultTo(responseTemplate), inputs)
    }.getOrElse("")
  }

}
