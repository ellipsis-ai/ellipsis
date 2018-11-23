package services.ms_teams.apiModels

sealed trait Attachment {
  val contentType: String
  val isFile: Boolean = false
}

case class LinkAttachment(
                       contentType: String,
                       contentUrl: String
                     ) extends Attachment

case class ContentAttachment(
                           contentType: String,
                           content: AttachmentContent,
                           contentUrl: Option[String],
                           name: Option[String]
                         ) extends Attachment {
  override val isFile: Boolean = contentType == "application/vnd.microsoft.teams.file.download.info"
}
