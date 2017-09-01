package models.behaviors.templates

import play.api.libs.json._

case class TemplateApplier(
                            maybeResponseTemplate: Option[String],
                            result: JsLookupResult,
                            inputs: Seq[(String, JsValue)] = Seq()
                            ) {

  def forceLineBreaks(s: String) : String = {
    s.replaceAll("""(?m)^(.+)\n""", "$1  \n")
  }

  def apply: String = {
    maybeResponseTemplate.filter(_.trim.nonEmpty).map { responseTemplate =>
      val newLinesPreserved = forceLineBreaks(responseTemplate)
      new TemplateParser().parseBlockFrom(newLinesPreserved).map { block =>
        val stringBuilder: StringBuilder = new StringBuilder()
        val renderer = MarkdownRenderer(stringBuilder, result, inputs)
        renderer.visit(block)
        stringBuilder.mkString
      }.getOrElse("")
    }.getOrElse(Json.prettyPrint(result.getOrElse(JsString(""))))
  }

}
