package models.behaviors.behaviorparameter

import play.api.libs.json.{JsNumber, JsString, JsValue}

import scala.concurrent.Future

sealed trait BehaviorParameterType {

  val name: String

  def isValid(text: String): Future[Boolean]

  def prepareForInvocation(text: String): Future[JsValue]

  val invalidPromptModifier: String

}

object TextType extends BehaviorParameterType {
  val name = "Text"

  def isValid(text: String) = Future.successful(true)

  def prepareForInvocation(text: String) = Future.successful(JsString(text))

  val invalidPromptModifier: String = "I need a valid answer"

}

object NumberType extends BehaviorParameterType {
  val name = "Number"

  def isValid(text: String) = Future.successful {
    try {
      text.toDouble
      true
    } catch {
      case e: NumberFormatException => false
    }
  }

  def prepareForInvocation(text: String) = Future.successful {
    try {
      JsNumber(BigDecimal(text))
    } catch {
      case e: NumberFormatException => JsString(text)
    }
  }

  val invalidPromptModifier: String = "I need a number"
}

object BehaviorParameterType {

  val all = Seq(
    TextType,
    NumberType
  )
  val allNames = all.map(_.name)

  def forName(name: String): BehaviorParameterType = all.find(_.name == name).getOrElse(TextType)
}
