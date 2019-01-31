package services.ms_teams.apiModels

import ai.x.play.json.Jsonx
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Formatting {

  lazy implicit val msTeamsOrganizationFormat: Format[MSTeamsOrganization] = Json.format[MSTeamsOrganization]
  lazy implicit val messageParticipantFormat = Json.format[MessageParticipantInfo]
  lazy implicit val conversationAccountFormat = Json.format[ConversationAccount]
  lazy implicit val tenantFormat = Json.format[TenantInfo]
  lazy implicit val channelDataFormat = Json.format[ChannelDataInfo]
  lazy implicit val activityFormat = Json.format[ActivityInfo]
  lazy implicit val responseFormat = Json.format[ResponseInfo]

  lazy implicit val adaptiveCardReads: Reads[AdaptiveCard] = Json.reads[AdaptiveCard]
  lazy implicit val adaptiveCardWrites: Writes[AdaptiveCard] = (
    (JsPath \ "body").write[Seq[CardElement]] and
      (JsPath \ "actions").write[Seq[CardElement]] and
      (JsPath \ "type").write[String] and
      (JsPath \ "$schema").write[String] and
      (JsPath \ "version").write[String]
    ) (a => (a.body, a.actions, a.`type`, a.`$schema`, a.version))
  lazy implicit val adaptiveCardFormat: Format[AdaptiveCard] = Format(adaptiveCardReads, adaptiveCardWrites)

  lazy implicit val fileFormat: Format[File] = Jsonx.formatCaseClass[File]
  lazy implicit val imageFormat: Format[Image] = Jsonx.formatCaseClass[Image]

  lazy implicit val unknownAttachmentContentFormat: Format[UnknownAttachmentContent] = Jsonx.formatInline[UnknownAttachmentContent]

  lazy implicit val actionSubmitReads: Reads[ActionSubmit] = Json.reads[ActionSubmit]
  lazy implicit val actionSubmitWrites: Writes[ActionSubmit] = (
    (JsPath \ "title").write[String] and
      (JsPath \ "data").write[JsValue] and
      (JsPath \ "type").write[String]
    ) (a => (a.title, a.data, a.`type`))
  lazy implicit val actionSubmitFormat: Format[ActionSubmit] = Format(actionSubmitReads, actionSubmitWrites)

  lazy implicit val inputChoiceReads: Reads[InputChoice] = Json.reads[InputChoice]
  lazy implicit val inputChoiceWrites: Writes[InputChoice] = (
    (JsPath \ "title").write[String] and
      (JsPath \ "value").write[String] and
      (JsPath \ "type").write[String]
    ) (c => (c.title, c.value, c.`type`))
  lazy implicit val inputChoiceFormat: Format[InputChoice] = Format(inputChoiceReads, inputChoiceWrites)

  lazy implicit val inputChoiceSetReads: Reads[InputChoiceSet] = Json.reads[InputChoiceSet]
  lazy implicit val inputChoiceSetWrites: Writes[InputChoiceSet] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "value").write[String] and
      (JsPath \ "choices").write[Seq[InputChoice]] and
      (JsPath \ "type").write[String]
    ) (s => (s.id, s.value, s.choices, s.`type`))
  lazy implicit val inputChoiceSetFormat: Format[InputChoiceSet] = Format(inputChoiceSetReads, inputChoiceSetWrites)

  lazy implicit val inputTextReads: Reads[InputText] = Json.reads[InputText]
  lazy implicit val inputTextWrites: Writes[InputText] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "type").write[String]
    ) (i => (i.id, i.`type`))
  lazy implicit val inputTextFormat: Format[InputText] = Format(inputTextReads, inputTextWrites)

  lazy implicit val textBlockReads: Reads[TextBlock] = Json.reads[TextBlock]
  lazy implicit val textBlockWrites: Writes[TextBlock] = (
    (JsPath \ "text").write[String] and
      (JsPath \ "type").write[String]
    ) (b => (b.text, b.`type`))
  lazy implicit val textBlockFormat: Format[TextBlock] = Format(textBlockReads, textBlockWrites)

  lazy implicit val channelAccountFormat: Format[ChannelAccount] = Jsonx.formatCaseClass[ChannelAccount]

  lazy implicit val mentionEntityReads: Reads[MentionEntity] = Json.reads[MentionEntity]
  lazy implicit val mentionEntityWrites: Writes[MentionEntity] = (
    (JsPath \ "mentioned").write[ChannelAccount] and
      (JsPath \ "text").write[String] and
      (JsPath \ "type").write[String]
    ) (m => (m.mentioned, m.text, m.`type`))
  lazy implicit val mentionEntityFormat: Format[MentionEntity] = Format(mentionEntityReads, mentionEntityWrites)

  lazy implicit val attachmentContentFormat: Format[AttachmentContent] = Jsonx.formatSealedWithFallback[AttachmentContent, UnknownAttachmentContent]
  lazy implicit val cardElementFormat: Format[CardElement] = Jsonx.formatSealed[CardElement]

  lazy implicit val attachmentFormat: Format[Attachment] = Json.format[Attachment]

  lazy implicit val applicationFormat: Format[Application] = Json.format[Application]

  lazy implicit val channelFormat: Format[Channel] = Json.format[Channel]
  lazy implicit val teamFormat: Format[Team] = Json.format[Team]
  lazy implicit val channelDataChannelFormat: Format[ChannelDataChannel] = Json.format[ChannelDataChannel]
  lazy implicit val channelDataTeamFormat: Format[ChannelDataTeam] = Json.format[ChannelDataTeam]
  lazy implicit val directoryObjectFormat: Format[DirectoryObject] = Json.format[DirectoryObject]
  lazy implicit val getPrivateConversationInfoFormat: Format[GetPrivateConversationInfo] = Json.format[GetPrivateConversationInfo]

  lazy implicit val mailBoxSettingsFormat: Format[MailBoxSettings] = Json.format[MailBoxSettings]
  lazy implicit val userFormat: Format[MSTeamsUser] = Json.format[MSTeamsUser]

}
