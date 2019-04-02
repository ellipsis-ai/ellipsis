package models.behaviors.messagelistener

import java.time.OffsetDateTime

import json.Formatting.actionArgFormat
import models.accounts.user.User
import models.behaviors.ActionArg
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.BehaviorParameter
import play.api.libs.json.Json
import services.AWSLambdaConstants

case class MessageListener(
                            id: String,
                            behavior: Behavior,
                            arguments: Seq[ActionArg],
                            medium: String,
                            channel: String,
                            maybeThreadId: Option[String],
                            user: User,
                            createdAt: OffsetDateTime
                            ) {
  def invocationParamsFor(params: Seq[BehaviorParameter], message: String): Map[String, String] = {
    params.flatMap { ea =>
      for {
        arg <- arguments.find(_.name == ea.input.name)
        value <- arg.value
      } yield {
        (AWSLambdaConstants.invocationParamFor(ea.rank - 1), value)
      }
    }.toMap
  }

  def toRaw: RawMessageListener = {
    RawMessageListener(
      id,
      behavior.id,
      Json.toJson(arguments),
      medium,
      channel,
      maybeThreadId,
      user.id,
      createdAt
    )
  }
}
