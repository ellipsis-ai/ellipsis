package models.behaviors.defaultstorageitem

import play.api.libs.json.{JsObject, JsValue, Json}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{InputObjectType, InputType}

object GraphQLHelpers {

  def fromInput(inputType: InputObjectType[JsObject]) = new FromInput[JsObject] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      JsObject(inputType.fields.map { ea =>
        ea.name -> Json.parse(ad(ea.name).toString)
      })
    }
  }

  def fromScalarInput[T](inputType: InputType[T]) = new FromInput[JsValue] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      Json.parse(node.asInstanceOf[String])
    }
  }

  def formatTypeName(name: String): String = {
    val withValidFirstChar = """$[^_a-zA-Z]*""".r.replaceAllIn(name, "")
    val withValidChars = """[^_a-zA-Z0-9\s]""".r.replaceAllIn(withValidFirstChar, "")
    val parts = withValidChars.split("""\s+""").map(ea => ea.charAt(0).toUpper.toString ++ ea.substring(1))
    parts.mkString("")
  }

  def formatFieldName(name: String): String = {
    val capitalized = formatTypeName(name)
    capitalized.charAt(0).toLower.toString ++ capitalized.substring(1)
  }

}
