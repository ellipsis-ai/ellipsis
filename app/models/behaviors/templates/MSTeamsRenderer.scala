package models.behaviors.templates

import org.commonmark.node.Link

class MSTeamsRenderer(stringBuilder: StringBuilder) extends ChatPlatformRenderer(stringBuilder) {

  val newline: String = "<br>"
  val emphasis: String = "**"
  val controlEntitiesToIgnoreRegex: String = "<[@#!].+>|<at>|</at>"

  protected def linkWithTitle(link: Link): Unit = {
    stringBuilder.append("[")
    visitChildren(link)
    stringBuilder.append("]")
    stringBuilder.append("(")
    stringBuilder.append(s"${link.getDestination}")
    stringBuilder.append(")")
  }

}
