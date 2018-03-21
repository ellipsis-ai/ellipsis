package utils

case class UploadFileSpec(
                          content: String,
                          filetype: Option[String],
                          filename: Option[String]
                        )
