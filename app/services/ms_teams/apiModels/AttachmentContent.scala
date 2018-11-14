package services.ms_teams.apiModels

sealed trait AttachmentContent {
  val `type`: String
  val $schema: String
  val version: String
}

case class AdaptiveCard(body: Seq[CardElement], actions: Seq[CardElement]) extends AttachmentContent{
  val `type`: String = "AdaptiveCard";
  val $schema: String = "http://adaptivecards.io/schemas/adaptive-card.json"
  val version: String = "1.0"
}
