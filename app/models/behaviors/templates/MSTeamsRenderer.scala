package models.behaviors.templates

class MSTeamsRenderer(stringBuilder: StringBuilder) extends ChatPlatformRenderer(stringBuilder) {

  val newline: String = "<br>"
  val emphasis: String = "**"
  val controlEntitiesToIgnoreRegex: String = "<[@#!].+>|<at>|</at>"

}
