package json

import java.time.OffsetDateTime

case class InvocationLogEntryData(
                                  behaviorId: String,
                                  resultType: String,
                                  messageText: String,
                                  resultText: String,
                                  context: String,
                                  maybeUserIdForContext: Option[String],
                                  runtimeInMilliseconds: Long,
                                  createdAt: OffsetDateTime
                                )
