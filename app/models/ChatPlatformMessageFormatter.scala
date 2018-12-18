package models

import java.util
import java.util.regex.Matcher

import json.UserData
import models.behaviors.templates.ChatPlatformRenderer
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.{AbstractVisitor, Image, Node}
import org.commonmark.parser.Parser

class CommonmarkVisitor extends AbstractVisitor {
  override def visit(image: Image) {
    image.unlink()
  }
}

trait ChatPlatformMessageFormatter {

  private val COMMONMARK_EXTENSIONS = util.Arrays.asList(StrikethroughExtension.create, AutolinkExtension.create)

  private def commonmarkParser = {
    Parser.builder().extensions(COMMONMARK_EXTENSIONS).build()
  }

  private def commonmarkNodeFor(text: String): Node = {
    val node = commonmarkParser.parse(text)
    node.accept(new CommonmarkVisitor())
    node
  }

  def convertUsernamesToLinks(formattedText: String, userList: Set[UserData]): String = {
    userList.toSeq.foldLeft(formattedText) { (text, user) =>
      val userNameLinkRegex = raw"""(^|\s|\W)<?@\Q${user.userNameOrDefault}\E>?($$|\s|\W)""".r
      val replacement = user.userIdForContext.map { userId =>
        s"<@${userId}>"
      }.getOrElse {
        s"@${user.userNameOrDefault}"
      }
      userNameLinkRegex.replaceAllIn(text, s"$$1${Matcher.quoteReplacement(replacement)}$$2")
    }
  }

  def newRendererFor(builder: StringBuilder): ChatPlatformRenderer

  def bodyTextFor(text: String, userList: Set[UserData]): String = {
    val builder = StringBuilder.newBuilder
    commonmarkNodeFor(text).accept(newRendererFor(builder))
    val result = builder.mkString
    val withUserIds = convertUsernamesToLinks(result, userList)
    withUserIds
  }

}
