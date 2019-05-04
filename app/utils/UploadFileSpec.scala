package utils

case class UploadFileSpec(
                           url: Option[String],
                           content: Option[String],
                           filetype: Option[String],
                           filename: Option[String]
                        )
