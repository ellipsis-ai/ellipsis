package models.behaviors.messagelistener

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.input.Input
import play.api.libs.json.Json
import services.AWSLambdaConstants

case class MessageListener(
                            id: String,
                            behavior: Behavior,
                            messageInputId: String,
                            arguments: Map[String, String],
                            medium: String,
                            channel: String,
                            maybeThreadId: Option[String],
                            user: User,
                            createdAt: OffsetDateTime
                            ) {
  def invocationParamsFor(params: Seq[BehaviorParameter], messageInput: Input, message: String): Map[String, String] = {
    params.flatMap { ea =>
      arguments.get(ea.input.name).map { paramValue =>
        (AWSLambdaConstants.invocationParamFor(ea.rank - 1), paramValue)
      }.orElse {
        if (messageInput.name == ea.input.name) {
          Some((AWSLambdaConstants.invocationParamFor(ea.rank - 1), message))
        } else {
          None
        }
      }
    }.toMap
  }

  def toRaw: RawMessageListener = {
    RawMessageListener(
      id,
      behavior.id,
      messageInputId,
      Json.toJson(arguments),
      medium,
      channel,
      maybeThreadId,
      user.id,
      createdAt
    )
  }
}
