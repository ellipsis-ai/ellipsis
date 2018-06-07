package services.slack.apiModels

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
                       text: String, `type`: String,
                       style: Option[String] = None,
                       value: Option[String] = None,
                       confirm: Option[ConfirmField] = None,
                       options: Option[Seq[ActionSelectOption]] = None
                      )

case class ConfirmField(text: String, title: Option[String] = None,
                        ok_text: Option[String] = None, cancel_text: Option[String] = None)

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
                       initial_comment: Option[SlackComment]
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
