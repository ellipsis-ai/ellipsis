package util

import play.api.libs.json._
import renderers.MarkdownRenderer
import utils.TemplateParser

case class TemplateApplier(
                            maybeResponseTemplate: Option[String],
                            result: JsLookupResult,
                            inputs: Seq[(String, JsValue)] = Seq()
                            ) {

  def apply: String = {
    maybeResponseTemplate.map { responseTemplate =>
      new TemplateParser().parseBlockFrom(responseTemplate).map { block =>
        val stringBuilder: StringBuilder = new StringBuilder()
        val renderer = MarkdownRenderer(stringBuilder, result, inputs)
        renderer.visit(block)
        stringBuilder.mkString
      }.getOrElse("")
    }.getOrElse("")
  }

}
