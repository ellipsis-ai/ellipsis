package models.behaviors.templates

import org.commonmark.node.Link

class SlackRenderer(stringBuilder: StringBuilder) extends ChatPlatformRenderer(stringBuilder) {

  val newline: String = "\r"
  val emphasis: String = "*"
  val controlEntitiesToIgnoreRegex: String = "<[@#!].+>"

  protected def linkWithTitle(link: Link): Unit = {
    stringBuilder.append("<")
    stringBuilder.append(s"${link.getDestination}")
    stringBuilder.append("|")
    visitChildren(link)
    stringBuilder.append(">")
  }

}
