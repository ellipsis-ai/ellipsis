package models

import models.behaviors.templates.{ChatPlatformRenderer, SlackRenderer}

object SlackMessageFormatter extends ChatPlatformMessageFormatter {
  def newRendererFor(builder: StringBuilder): ChatPlatformRenderer = new SlackRenderer(builder)
}
