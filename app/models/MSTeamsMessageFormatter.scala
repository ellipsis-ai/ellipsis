package models

import models.behaviors.templates.{ChatPlatformRenderer, MSTeamsRenderer}

object MSTeamsMessageFormatter extends ChatPlatformMessageFormatter {
  def newRendererFor(builder: StringBuilder): ChatPlatformRenderer = new MSTeamsRenderer(builder)
}
