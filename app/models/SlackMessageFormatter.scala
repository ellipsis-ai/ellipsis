package models

import java.util
import java.util.regex.Matcher

import models.behaviors.events.MessageUserData
import models.behaviors.templates.SlackRenderer
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.{AbstractVisitor, Image, Node}
import org.commonmark.parser.Parser

class CommonmarkVisitor extends AbstractVisitor {
  override def visit(image: Image) {
    image.unlink()
  }
}

object SlackMessageFormatter {

  private val COMMONMARK_EXTENSIONS = util.Arrays.asList(StrikethroughExtension.create, AutolinkExtension.create)

  private def commonmarkParser = {
    Parser.builder().extensions(COMMONMARK_EXTENSIONS).build()
  }

  private def commonmarkNodeFor(text: String): Node = {
    val node = commonmarkParser.parse(text)
    node.accept(new CommonmarkVisitor())
    node
  }

  def convertUsernamesToLinks(formattedText: String, userList: Set[MessageUserData]): String = {
    userList.toSeq.foldLeft(formattedText) { (text, user) =>
      val userNameLinkRegex = raw"""(^|\s|\W)<?@\Q${user.userName}\E>?($$|\s|\W)""".r
      val replacement = user.userIdForContext.map { userId =>
        s"<@${userId}>"
      }.getOrElse {
        s"@${user.userName}"
      }
      userNameLinkRegex.replaceAllIn(text, s"$$1${Matcher.quoteReplacement(replacement)}$$2")
    }
  }

  def bodyTextFor(text: String, userList: Set[MessageUserData]): String = {
    val builder = StringBuilder.newBuilder
    val slack = new SlackRenderer(builder)
    commonmarkNodeFor(text).accept(slack)
    val result = builder.mkString
    val withUserIds = convertUsernamesToLinks(result, userList)
    withUserIds
  }

}
