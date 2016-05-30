package util

import play.api.libs.json._
import services.AWSLambdaConstants._

import scala.util.matching.Regex

case class TemplateApplier(maybeResponseTemplate: Option[String], result: JsLookupResult) {


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

  private def applyToObject(result: JsLookupResult): String = {
    maybeResponseTemplate.map { responseTemplate =>
      val pathsRegex = s"""(?s)\\{$RESULT_KEY(\\.\\S+)?\\}""".r
      pathsRegex.replaceAllIn(responseTemplate, pathReplacement(result))
    }.getOrElse("")
  }

  def apply: String = applyToObject(result)

}
