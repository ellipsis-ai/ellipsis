package services.ms_teams.apiModels

import play.api.libs.json._

object Formatting {

  lazy implicit val msTeamsOrganizationFormat: Format[MSTeamsOrganization] = Json.format[MSTeamsOrganization]
  lazy implicit val messageParticipantFormat = Json.format[MessageParticipantInfo]
  lazy implicit val conversationFormat = Json.format[ConversationInfo]
  lazy implicit val tenantFormat = Json.format[TenantInfo]
  lazy implicit val channelDataFormat = Json.format[ChannelDataInfo]
  lazy implicit val activityFormat = Json.format[ActivityInfo]
  lazy implicit val responseFormat = Json.format[ResponseInfo]
  lazy implicit val linkAttachmentFormat = Json.format[LinkAttachment]
  lazy implicit val contentAttachmentFormat = Json.format[ContentAttachment]

}
