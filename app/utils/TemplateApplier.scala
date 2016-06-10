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

  private def lookUp(obj: JsLookupResult, properties: Array[String]): JsLookupResult = {
    properties.headOption.map { property =>
      lookUp(obj \ property, properties.tail)
    }.getOrElse {
      obj
    }
  }

  private def lookUp(obj: JsLookupResult, path: String): JsLookupResult = {
    val segments = path.split("\\.").filter(_.nonEmpty)
    lookUp(result, segments)
  }

  private def pathReplacement(result: JsLookupResult): Regex.Match => String = {
    m: Regex.Match =>
      m.subgroups.headOption.map { path =>
        if (path == null) {
          result.toOption.map(printJsValue).getOrElse("not found")
        } else {
          lookUp(result, path).toOption.map(printJsValue).getOrElse(s"$RESULT_KEY$path not found")
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

  def applyIterationTo(responseTemplate: String): String = {
    val iterationRegex = """(?s)\{\s*for\s+(\S+)\s+in\s+(\S+)\s*\}(.*?)\{\s*endfor\s*\}""".r
    iterationRegex.replaceAllIn(responseTemplate, iterationMatch => {
      val captured = iterationMatch.subgroups
      val itemName = captured.head
      val listName = captured(1)
      val itemTemplate = captured(2)
      s"""$RESULT_KEY(\\..*)?""".r.findFirstMatchIn(listName).flatMap { listNameMatch =>
        val listNameWithoutResult = listNameMatch.subgroups.head
        val listValue = if (listNameWithoutResult == null) {
          result
        } else {
          lookUp(result, listNameWithoutResult)
        }
        listValue.asOpt[Array[String]].map { items =>
          items.map { item =>
            s"""\\{\\s*$itemName\\s*\\}""".r.replaceAllIn(itemTemplate, item)
          }.mkString("")
        }
      }.getOrElse(iterationMatch.source.toString)
    })
  }

  def apply: String = {
    maybeResponseTemplate.map { responseTemplate =>
      applyInputsTo(applyResultTo(applyIterationTo(responseTemplate)), inputs)
    }.getOrElse("")
  }

}
