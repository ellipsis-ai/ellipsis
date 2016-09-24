package models.data.apibackeddatatype

import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterType}
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.team.Team
import org.joda.time.DateTime
import play.api.libs.json.{JsString, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ApiBackedDataType(
                              id: String,
                              name: String,
                              team: Team,
                              maybeCurrentVersionId: Option[String],
                              maybeImportedId: Option[String],
                              createdAt: DateTime
                            ) extends BehaviorParameterType {

  def isValid(text: String): Future[Boolean] = Future.successful(true)

  def prepareForInvocation(text: String): Future[JsValue] = Future.successful(JsString(text))

  def invalidPromptModifier: String = s"I need a $name"

  override def promptFor(parameter: BehaviorParameter, maybePreviousCollectedValue: Option[CollectedParameterValue]): Future[String] = {
    for {
      superPrompt <- super.promptFor(parameter, maybePreviousCollectedValue)
      choicesPrompt <- Future.successful("\n\n<no choices")
    } yield superPrompt ++ choicesPrompt
  }

}
