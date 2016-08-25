package models.bots.events

case class SlackMessageEvent(context: SlackMessageContext) extends MessageEvent
