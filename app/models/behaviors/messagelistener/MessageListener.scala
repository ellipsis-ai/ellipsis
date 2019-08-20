package models.behaviors.messagelistener

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.BehaviorParameter
import play.api.libs.json.Json
import services.AWSLambdaConstants

case class MessageListener(
                            id: String,
                            behavior: Behavior,
                            arguments: Map[String, String],
                            medium: String,
                            channel: String,
                            maybeThreadId: Option[String],
                            user: User,
                            isForCopilot: Boolean,
                            isEnabled: Boolean,
                            createdAt: OffsetDateTime,
                            maybeLastCopilotActivityAt: Option[OffsetDateTime]
                            ) {
  def invocationParamsFor(params: Seq[BehaviorParameter], message: String): Map[String, String] = {
    params.flatMap { ea =>
      arguments.get(ea.input.name).map { paramValue =>
        (AWSLambdaConstants.invocationParamFor(ea.rank - 1), paramValue)
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
      isForCopilot,
      isEnabled,
      createdAt,
      maybeLastCopilotActivityAt
    )
  }
}
