package models.behaviors.testing

import models.behaviors.events.MessageEvent

case class TestEvent(context: TestMessageContext) extends MessageEvent
