package models.behaviors.events

case class SlackMessageEvent(context: SlackMessageContext) extends MessageEvent
