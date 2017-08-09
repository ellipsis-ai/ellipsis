package utils

case class SlackFileSpec(
                          content: Option[String],
                          filetype: Option[String],
                          filename: Option[String]
                        )
