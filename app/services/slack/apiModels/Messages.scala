package services.slack.apiModels

import utils.JsonEnumValue

case class Attachment (
                        fallback: Option[String] = None,
                        callback_id: Option[String] = None,
                        color: Option[String] = None,
                        pretext: Option[String] = None,
                        author_name: Option[String] = None,
                        author_link: Option[String] = None,
                        author_icon: Option[String] = None,
                        title: Option[String] = None,
                        title_link: Option[String] = None,
                        text: Option[String] = None,
                        fields: Seq[AttachmentField] = Seq.empty,
                        image_url: Option[String] = None,
                        thumb_url: Option[String] = None,
                        actions: Seq[ActionField] = Seq.empty,
                        mrkdwn_in: Seq[String] = Seq.empty
                      )

case class AttachmentField(title: String, value: String, short: Boolean)

case class ActionSelectOption(text: String, value: String)

case class ActionField(name: String,
                       text: String,
                       `type`: String,
                       style: Option[String] = None,
                       value: Option[String] = None,
                       confirm: Option[ConfirmField] = None,
                       options: Option[Seq[ActionSelectOption]] = None
                      )

object BlockType extends utils.JsonEnum[BlockType] {
  val values = List(SectionBlockType, DividerBlockType, ImageBlockType, ActionsBlockType, ContextBlockType)
}

sealed trait BlockType extends BlockType.Value with JsonEnumValue

case object SectionBlockType extends BlockType { val value = "section" }
case object DividerBlockType extends BlockType { val value = "divider" }
case object ImageBlockType extends BlockType { val value = "image" }
case object ActionsBlockType extends BlockType { val value = "actions" }
case object ContextBlockType extends BlockType { val value = "context" }

object BlockElementType extends utils.JsonEnum[BlockElementType] {
  val values = List(
    ImageElementType,
    ButtonElementType,
    StaticSelectElementType,
    DynamicSelectElementType,
    UserSelectElementType,
    ConversationSelectElementType,
    ChannelSelectElementType,
    OverflowElementType,
    DatePickerElementType
  )
}

sealed trait BlockElementType extends BlockElementType.Value with JsonEnumValue

case object ImageElementType extends BlockElementType { val value = "image" }
case object ButtonElementType extends BlockElementType { val value = "button" }
case object StaticSelectElementType extends BlockElementType { val value = "static_select" }
case object DynamicSelectElementType extends BlockElementType { val value = "external_select" }
case object UserSelectElementType extends BlockElementType { val value = "users_select" }
case object ConversationSelectElementType extends BlockElementType { val value = "conversations_select" }
case object ChannelSelectElementType extends BlockElementType { val value = "channels_select" }
case object OverflowElementType extends BlockElementType { val value = "overflow" }
case object DatePickerElementType extends BlockElementType { val value = "datepicker" }

object ButtonStyle extends utils.JsonEnum[ButtonStyle] {
  val values = List(PrimaryButtonStyle, DangerButtonStyle)
}

sealed trait ButtonStyle extends ButtonStyle.Value with JsonEnumValue

case object PrimaryButtonStyle extends ButtonStyle { val value = "primary" }
case object DangerButtonStyle extends ButtonStyle { val value = "danger" }

sealed trait BlockElement {
  val `type`: BlockElementType
}

case class ButtonElement(
                          text: String,
                          action_id: String,
                          url: Option[String],
                          value: Option[String],
                          style: Option[ButtonStyle],
                          confirm: Option[ConfirmDialog]
                        ) extends BlockElement {
  val `type`: BlockElementType = ButtonElementType
}

case class ConfirmDialog(title: PlainTextObject, text: TextObject, confirm: PlainTextObject, deny: PlainTextObject)

object TextObjectType extends utils.JsonEnum[TextObjectType] {
  val values = List(PlainTextType, MarkdownType)
}

sealed trait TextObjectType extends TextObjectType.Value with JsonEnumValue
case object PlainTextType extends TextObjectType { val value = "plain_text" }
case object MarkdownType extends TextObjectType { val value = "mrkdwn" }

trait TextObject {
  val `type`: TextObjectType
  val text: String
  val emoji: Option[Boolean]
  val verbatim: Option[Boolean]
}

case class PlainTextObject(text: String, emoji: Option[Boolean]) extends TextObject {
  val `type`: TextObjectType = PlainTextType
  val verbatim: Option[Boolean] = None
}

case class MarkdownTextObject(text: String, verbatim: Option[Boolean]) extends TextObject {
  val `type`: TextObjectType = MarkdownType
  val emoji: Option[Boolean] = None
}

sealed trait Block { val `type`: BlockType }

case class ActionsBlock(elements: Seq[BlockElement], block_id: String) extends Block {
  val `type`: BlockType = ActionsBlockType
}

case class ConfirmField(
                         text: String,
                         title: Option[String] = None,
                         ok_text: Option[String] = None,
                         cancel_text: Option[String] = None
                       )

case class SlackComment (
                          id: String,
                          timestamp: Long,
                          user: String,
                          comment: String
                        )

case class SlackFile (
                       id: String,
                       created: Long,
                       timestamp: Long,
                       name: Option[String],
                       title: String,
                       mimetype: String,
                       filetype: String,
                       pretty_type: String,
                       user: String,
                       mode: String,
                       editable: Boolean,
                       is_external: Boolean,
                       external_type: String,
                       size: Long,
                       url: Option[String],
                       url_download: Option[String],
                       url_private: Option[String],
                       url_private_download: Option[String],
                       initial_comment: Option[SlackComment],
                       permalink: Option[String]
                       //thumb_64: Option[String],
                       //thumb_80: Option[String],
                       //thumb_360: Option[String],
                       //thumb_360_gif: Option[String],
                       //thumb_360_w: Option[String],
                       //thumb_360_h: Option[String],
                       //permalink: String,
                       //edit_link: Option[String],
                       //preview: Option[String],
                       //preview_highlight: Option[String],
                       //lines: Option[Int],
                       //lines_more: Option[Int],
                       //is_public: Boolean,
                       //public_url_shared: Boolean,
                       //channels: Seq[String],
                       //groups: Option[Seq[String]],
                       //num_stars: Option[Int],
                       //is_starred: Option[Boolean]
                     )
