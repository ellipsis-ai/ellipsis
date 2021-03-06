package models.behaviors.templates

import play.api.libs.json._

case class TemplateApplier(
                            maybeResponseTemplate: Option[String],
                            result: JsLookupResult,
                            inputs: Seq[(String, JsValue)] = Seq()
                            ) {

  def apply: String = {
    maybeResponseTemplate.filter(_.trim.nonEmpty).map { responseTemplate =>
      new TemplateParser().parseBlockFrom(responseTemplate).map { block =>
        val stringBuilder: StringBuilder = new StringBuilder()
        val renderer = MarkdownRenderer(stringBuilder, result, inputs)
        renderer.visit(block)
        stringBuilder.mkString
      }.getOrElse("")
    }.getOrElse {
      val jsValue = result.getOrElse(JsString(""))
      jsValue match {
        case s: JsString => s.value
        case _ => Json.prettyPrint(jsValue)
      }
    }
  }

}
