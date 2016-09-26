package models.behaviors.behaviorparameter

import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.events.MessageEvent
import models.data.apibackeddatatype.ApiBackedDataType
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait BehaviorParameterType {

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

  def promptFor(
                  parameter: BehaviorParameter,
                  maybePreviousCollectedValue: Option[CollectedParameterValue],
                  event: MessageEvent,
                  dataService: DataService,
                  lambdaService: AWSLambdaService,
                  ws: WSClient
               ): Future[String] = {
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

case class ApiBackedBehaviorParameterType(
                                           id: String,
                                           name: String,
                                           dataType: ApiBackedDataType
                                         ) extends BehaviorParameterType {

  def isValid(text: String): Future[Boolean] = Future.successful(true)

  def prepareForInvocation(text: String): Future[JsValue] = Future.successful(JsString(text))

  def invalidPromptModifier: String = s"I need a $name"

  override def promptFor(
                          parameter: BehaviorParameter,
                          maybePreviousCollectedValue: Option[CollectedParameterValue],
                          event: MessageEvent,
                          dataService: DataService,
                          lambdaService: AWSLambdaService,
                          ws: WSClient
                        ): Future[String] = {
    for {
      superPrompt <- super.promptFor(parameter, maybePreviousCollectedValue, event, dataService, lambdaService, ws)
      maybeCurrentVersion <- dataType.maybeCurrentVersionId.map { id =>
        dataService.apiBackedDataTypeVersions.find(id)
      }.getOrElse(Future.successful(None))
      validValues <- maybeCurrentVersion.map { version =>
        version.validValuesFor(None, event, ws, lambdaService)
      }.getOrElse(Future.successful(Seq()))
    } yield superPrompt ++ validValues.map(_.label).mkString("\n-")
  }

}

object ApiBackedBehaviorParameterType {

  def buildFor(dataType: ApiBackedDataType, dataService: DataService): Future[ApiBackedBehaviorParameterType] = {
    val eventualMaybeCurrentVersion = dataType.maybeCurrentVersionId.map { currentVersionId =>
      dataService.apiBackedDataTypeVersions.find(currentVersionId)
    }.getOrElse(Future.successful(None))

    eventualMaybeCurrentVersion.map { maybeCurrentVersion =>
      val name = maybeCurrentVersion.map(_.name).getOrElse("Unnamed type")
      ApiBackedBehaviorParameterType(dataType.id, name, dataType)
    }
  }

}


object BehaviorParameterType {

  val allBuiltIn = Seq(
    TextType,
    NumberType
  )
  val allNames = allBuiltIn.map(_.name)

  def forName(name: String): BehaviorParameterType = allBuiltIn.find(_.name == name).getOrElse(TextType)
}
