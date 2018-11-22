package services.ms_teams.apiModels

import utils.FileReference

sealed trait AttachmentContent {

}

case class AdaptiveCard(body: Seq[CardElement], actions: Seq[CardElement]) extends AttachmentContent{
  val `type`: String = "AdaptiveCard";
  val $schema: String = "http://adaptivecards.io/schemas/adaptive-card.json"
  val version: String = "1.0"
}

case class File(downloadUrl: String, uniqueId: String, fileType: String) extends AttachmentContent with FileReference {
  val url: String = downloadUrl
  val maybeThumbnailUrl: Option[String] = None
}
