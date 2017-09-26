package models.behaviors.events
import slack.models.Attachment
import utils.Color

case class SlackMessageTextAttachments(
                                        text: String
                                      ) extends SlackMessageAttachments {

  val attachments: Seq[Attachment] = {
    Seq(Attachment(
      fallback = Some("This feature is unavailable on this platform."),
      color = Some(Color.BLUE_LIGHT),
      mrkdwn_in = Seq("text"),
      text = Some(text)
    ))
  }
}
