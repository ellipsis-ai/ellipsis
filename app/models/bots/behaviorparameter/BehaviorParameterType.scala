package models.bots.behaviorparameter

import play.api.libs.json.{JsNumber, JsString, JsValue}

sealed trait BehaviorParameterType {

  val name: String

  def isValid(text: String): Boolean

  def prepareForInvocation(text: String): JsValue

}

class TextType extends BehaviorParameterType {
  val name = BehaviorParameterType.TEXT

  def isValid(text: String) = true

  def prepareForInvocation(text: String): JsValue = JsString(text)

}

class NumberType extends BehaviorParameterType {
  val name = BehaviorParameterType.NUMBER

  def isValid(text: String): Boolean = try {
    text.toDouble
    true
  } catch {
    case e: NumberFormatException => false
  }

  def prepareForInvocation(text: String): JsValue = try {
    JsNumber(BigDecimal(text))
  } catch {
    case e: NumberFormatException => JsString(text)
  }
}

object BehaviorParameterType {

  val TEXT = "Text"
  val NUMBER = "Number"

  val allNames = Seq(
    TEXT,
    NUMBER
  )

  def forName(name: String): BehaviorParameterType = {
    name match {
      case TEXT => new TextType()
      case NUMBER => new NumberType()
      case _ => new TextType()
    }
  }
}
