package services.ms_teams.apiModels

import ai.x.play.json.Jsonx
import play.api.libs.json._

object Formatting {

  import json.Formatting._

  lazy implicit val msTeamsOrganizationFormat: Format[MSTeamsOrganization] = Json.format[MSTeamsOrganization]
  lazy implicit val messageParticipantFormat = Json.format[MessageParticipantInfo]
  lazy implicit val conversationFormat = Json.format[ConversationInfo]
  lazy implicit val tenantFormat = Json.format[TenantInfo]
  lazy implicit val channelDataFormat = Json.format[ChannelDataInfo]
  lazy implicit val activityFormat = Json.format[ActivityInfo]
  lazy implicit val responseFormat = Json.format[ResponseInfo]
  lazy implicit val adaptiveCardFormat: Format[AdaptiveCard] = new Format[AdaptiveCard] {
    def writes(c: AdaptiveCard) = Json.obj(
      "type" -> c.`type`,
      "$schema" -> c.$schema,
      "version" -> c.version,
      "body" -> c.body,
      "actions" -> c.actions
    )
    def reads(json: JsValue): JsResult[AdaptiveCard] = {
      (json \ "actions").validate[Seq[CardAction]] match {
        case JsSuccess(actions, _) => JsSuccess(AdaptiveCard(Seq(), actions))
        case JsError(errors) => JsError(errors)
      }
    }
  }
  lazy implicit val cardActionFormat: Format[CardAction] = Jsonx.formatCaseClass[CardAction]
  lazy implicit val textBlockFormat: Format[TextBlock] = Jsonx.formatCaseClass[TextBlock]
  lazy implicit val attachmentContentFormat: Format[AttachmentContent] = Jsonx.formatSealed[AttachmentContent]
  lazy implicit val cardElementFormat: Format[CardElement] = Jsonx.formatSealed[CardElement]
  lazy implicit val contentAttachmentFormat: Format[ContentAttachment] = Jsonx.formatCaseClass[ContentAttachment]

  lazy implicit val linkAttachmentFormat: Format[LinkAttachment] = Jsonx.formatCaseClass[LinkAttachment]
  lazy implicit val attachmentFormat: Format[Attachment] = Jsonx.formatSealed[Attachment]

}
