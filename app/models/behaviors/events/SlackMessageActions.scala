package models.behaviors.events

import slack.models.Attachment

case class SlackMessageActions(actions: Seq[SlackMessageAction]) extends MessageActions {

  type T = SlackMessageAction

  lazy val attachment: Attachment = {
    Attachment(actions = actions.map(_.actionField))
  }

}
