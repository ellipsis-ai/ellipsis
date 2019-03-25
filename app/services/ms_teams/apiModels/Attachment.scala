package services.ms_teams.apiModels

case class Attachment(
                       contentType: String,
                       content: Option[AttachmentContent],
                       contentUrl: Option[String],
                       name: Option[String]
                     ) {
  val isFile: Boolean = contentType == "application/vnd.microsoft.teams.file.download.info"
  val isHtml: Boolean = contentType == "text/html"
}
