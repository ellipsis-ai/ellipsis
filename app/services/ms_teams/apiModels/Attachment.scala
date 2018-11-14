package services.ms_teams.apiModels

sealed trait Attachment {
  val contentType: String
}

case class LinkAttachment(
                       contentType: String,
                       contentUrl: String
                     ) extends Attachment

case class ContentAttachment(
                           contentType: String,
                           content: AttachmentContent
                         ) extends Attachment
