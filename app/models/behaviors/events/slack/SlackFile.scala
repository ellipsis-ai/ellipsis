package models.behaviors.events.slack

import utils.FileReference

case class SlackFile(url: String, maybeThumbnailUrl: Option[String]) extends FileReference
