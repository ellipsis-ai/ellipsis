package models.behaviors.behaviorparameter

import com.fasterxml.jackson.core.JsonParseException
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.data.apibackeddatatype.{ApiBackedDataTypeValue, ApiBackedDataTypeVersion}
import play.api.libs.json._

import scala.concurrent.Future

trait BehaviorParameterType {

  def id: String
  def name: String

  def asJson: JsValue = Json.toJson(Map("id" -> id, "name" -> name))

  def isValid(text: String): Future[Boolean]

  def prepareForInvocation(text: String): Future[JsValue]

  def invalidValueModifierFor(maybePreviousCollectedValue: Option[CollectedParameterValue]): String = {
    if (maybePreviousCollectedValue.isDefined) {
      s" (${invalidPromptModifier})"
    } else {
      ""
    }
  }

  def promptFor(parameter: BehaviorParameter, maybePreviousCollectedValue: Option[CollectedParameterValue]): Future[String] = {
    Future.successful(s"${parameter.question}${invalidValueModifierFor(maybePreviousCollectedValue)}")
  }

  def invalidPromptModifier: String

}

trait BuiltinType extends BehaviorParameterType {
  val id = name
}

object TextType extends BuiltinType {
  val name = "Text"

  def isValid(text: String) = Future.successful(true)

  def prepareForInvocation(text: String) = Future.successful(JsString(text))

  def invalidPromptModifier: String = "I need a valid answer"

}

object NumberType extends BuiltinType {
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

  def invalidPromptModifier: String = "I need a number"
}

//case class ApiBackedParameterType(dataTypeVersion: ApiBackedDataTypeVersion) extends BehaviorParameterType {
//
//  import models.data.apibackeddatatype.ApiBackedDataTypeValue._
//
//  val id = dataTypeVersion.id
//  val name = dataTypeVersion.name
//
//  def isValid(text: String): Future[Boolean] = Future.successful(true)
//
//  def prepareForInvocation(text: String) = Future.successful {
//    try {
//      Json.parse(text).validate[ApiBackedDataTypeValue] match {
//        case JsSuccess(data, jsPath) => JsString(data.id)
//        case e: JsError => JsString(text)
//      }
//    } catch {
//      case e: JsonParseException => JsString(text)
//    }
//  }
//
//  def invalidPromptModifier: String = s"I need a ${dataTypeVersion.name}"
//}

object BehaviorParameterType {

  val allBuiltIn = Seq(
    TextType,
    NumberType
  )
  val allNames = allBuiltIn.map(_.name)

  def forName(name: String): BehaviorParameterType = allBuiltIn.find(_.name == name).getOrElse(TextType)
}
