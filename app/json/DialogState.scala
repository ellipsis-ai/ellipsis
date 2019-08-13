package json

case class DialogState(
                        maybeMessageId: Option[String],
                        maybeThreadId: Option[String],
                        arguments: Map[String, String]
                      )
