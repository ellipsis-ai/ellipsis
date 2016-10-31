package json

import org.joda.time.DateTime

case class InvocationLogEntryData(
                                  behaviorId: String,
                                  resultType: String,
                                  messageText: String,
                                  resultText: String,
                                  context: String,
                                  maybeUserIdForContext: Option[String],
                                  runtimeInMilliseconds: Long,
                                  createdAt: DateTime
                                )
