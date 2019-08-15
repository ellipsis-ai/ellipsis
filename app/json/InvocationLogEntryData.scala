package json

import java.time.OffsetDateTime

import models.behaviors.invocationlogentry.InvocationLogEntry

case class InvocationLogEntryData(
                                   id: String,
                                   behaviorId: String,
                                   resultType: String,
                                   messageText: String,
                                   resultText: String,
                                   context: String,
                                   maybeChannel: Option[String],
                                   maybeUserIdForContext: Option[String],
                                   maybeOriginalEventType: Option[String],
                                   runtimeInMilliseconds: Long,
                                   createdAt: OffsetDateTime
                                 )

object InvocationLogEntryData {
  def from(entry: InvocationLogEntry): InvocationLogEntryData = {
    InvocationLogEntryData(
      entry.id,
      entry.behaviorVersion.behavior.id,
      entry.resultType,
      entry.messageText,
      entry.resultText,
      entry.context,
      entry.maybeChannel,
      entry.maybeUserIdForContext,
      entry.maybeOriginalEventType.map(_.toString),
      entry.runtimeInMilliseconds,
      entry.createdAt
    )
  }
}
