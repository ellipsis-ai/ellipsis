package models.behaviors.templates

class SlackRenderer(stringBuilder: StringBuilder) extends ChatPlatformRenderer(stringBuilder) {

  val newline: String = "\r"
  val emphasis: String = "*"
  val controlEntitiesToIgnoreRegex: String = "<[@#!].+>"

}
