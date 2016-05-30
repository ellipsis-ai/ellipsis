package util

import play.api.libs.json._
import services.AWSLambdaConstants._

import scala.util.matching.Regex

case class TemplateApplier(maybeResponseTemplate: Option[String], result: JsLookupResult) {

  private def applyToString(result: String): String = {
    maybeResponseTemplate.map { responseTemplate =>
      s"""\\{$RESULT_KEY\\}""".r.replaceAllIn(responseTemplate, result)
    }.getOrElse(result)
  }

  private def lookUp(obj: JsLookupResult, properties: Array[String]): String = {
    properties.headOption.map { property =>
      lookUp(obj \ property, properties.tail)
    }.getOrElse {
      obj.toOption.map { value =>
       value match {
         case s: JsString => s.value
         case _ => value.toString
       }
      }.getOrElse("Not found")
    }
  }

  private def pathReplacement(result: JsLookupResult): Regex.Match => String = {
    m: Regex.Match =>
      m.subgroups.headOption.map { path =>
        if (path == null) {
          result.toOption.map(_.toString).getOrElse("")
        } else {
          val segments = path.split("\\.").filter(_.nonEmpty)
          lookUp(result, segments)
        }
      }.getOrElse(m.toString())
  }

  private def applyToObject(result: JsLookupResult): String = {
    maybeResponseTemplate.map { responseTemplate =>
      val pathsRegex = s"""(?s)\\{$RESULT_KEY(\\.\\S+)?\\}""".r
      pathsRegex.replaceAllIn(responseTemplate, pathReplacement(result))
    }.getOrElse("")
  }

  def apply: String = {
    result match {
      case JsDefined(s: JsString) => applyToString(s.as[String])
      case _ => applyToObject(result)
    }
  }

}
