package services.slack.apiModels

import play.api.libs.json._

object Formatting {

  def enumTypedToJsObject[E <: EnumTyped](e: E, j: JsObject): JsObject = {
    val nonNullFields = (Json.obj("type" -> e.`type`.value) ++ j).fields.filter {
      case (_, jsv) => jsv != JsNull
    }
    JsObject(nonNullFields)
  }
  lazy implicit val slackCommentFormat: Format[SlackComment] = Json.format[SlackComment]
  lazy implicit val slackFileFormat: Format[SlackFile] = Json.format[SlackFile]
  lazy implicit val actionSelectOptionFormat: Format[ActionSelectOption] = Json.format[ActionSelectOption]
  lazy implicit val confirmFieldFormat: Format[ConfirmField] = Json.format[ConfirmField]
  lazy implicit val actionFieldFormat: Format[ActionField] = Json.format[ActionField]
  lazy implicit val attachmentFieldFormat: Format[AttachmentField] = Json.format[AttachmentField]
  lazy implicit val attachmentFormat: Format[Attachment] = Json.format[Attachment]
  lazy implicit val slackUserProfileFormat: Format[SlackUserProfile] = Json.format[SlackUserProfile]
  lazy implicit val slackEnterpriseUserFormat: Format[SlackEnterpriseUser] = Json.format[SlackEnterpriseUser]
  lazy implicit val slackUserFormat: Format[SlackUser] = Json.format[SlackUser]
  lazy implicit val slackTeamFormat: Format[SlackTeam] = Json.format[SlackTeam]
  lazy implicit val membershipDataFormat: Format[MembershipData] = Json.format[MembershipData]
  lazy implicit val textObjectFormat: Writes[TextObject] = new Writes[TextObject] {
    override def writes(o: TextObject): JsValue = enumTypedToJsObject(o, Json.obj(
      "text" -> o.text,
      "emoji" -> o.emoji,
      "verbatim" -> o.verbatim
    ))
  }
  lazy implicit val confirmDialogWrites: Writes[ConfirmDialog] = Json.writes[ConfirmDialog]
  lazy implicit val selectElementOptionWrites: Writes[SelectElementOption] = Json.writes[SelectElementOption]
  lazy implicit val selectElementOptionGroupWrites: Writes[SelectElementOptionGroup] = Json.writes[SelectElementOptionGroup]
  lazy implicit val interactiveBlockElementWrites: Writes[InteractiveBlockElement] = new Writes[InteractiveBlockElement] {
    override def writes(o: InteractiveBlockElement): JsValue = {
      o match {
        case b: ButtonElement => enumTypedToJsObject(b, Json.obj(
          "text" -> b.text,
          "action_id" -> b.action_id,
          "url" -> b.url,
          "value" -> b.value,
          "style" -> b.style.map(_.value),
          "confirm" -> b.confirm
        ))
        case s: StaticSelectElement => enumTypedToJsObject(s, Json.obj(
          "placeholder" -> s.placeholder,
          "action_id" -> s.action_id,
          "options" -> s.options,
          "option_groups" -> s.option_groups,
          "initial_option" -> s.initial_option,
          "confirm" -> s.confirm
        ))
      }
    }
  }
  lazy implicit val blockWrites: Writes[Block] = new Writes[Block] {
    override def writes(o: Block): JsValue = {
      o match {
        case s: SectionBlock => enumTypedToJsObject(s, Json.obj(
          "text" -> s.text
        ))
        case a: ActionsBlock => enumTypedToJsObject(a, Json.obj(
          "elements" -> a.elements,
          "block_id" -> a.block_id
        ))
      }
    }
  }
}
