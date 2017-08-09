package models

import java.util

import models.accounts.slack.SlackUserInfo
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

  def bodyTextFor(text: String): String = {
    val builder = StringBuilder.newBuilder
    val slack = new SlackRenderer(builder)
    commonmarkNodeFor(text).accept(slack)
    builder.mkString
  }

  def unformatLinks(text: String): String = {
    text.
      replaceAll("""<@(?:.+?\|)?(.+?)>""", "@$1").
      replaceAll("""<#(?:.+?\|)?(.+?)>""", "#$1").
      replaceAll("""<!(here|group|channel|everyone)(\|(here|group|channel|everyone))?>""", "@$1").
      replaceAll("""<!subteam\^.+?\|(.+?)>""", "@$1").
      replaceAll("""<!date.+?\|(.+?)>""", "$1").
      replaceAll("""<(?:[^!].*?\|)(.+?)>""", "$1").
      replaceAll("""<([^!].*?)>""", "$1").
      replaceAll("""<!(?:.+?\|)?(.+?)>""", "<$1>")
  }

  def unescapeSlackHTMLEntities(text: String): String = {
    text.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
  }

  def augmentUserIdsWithNames(initialText: String, userList: Seq[SlackUserInfo]): String = {
    userList.foldLeft(initialText) { (resultText, user) =>
      resultText.replace(s"""<@${user.userId}>""", s"""<@${user.userId}|${user.name}>""")
    }
  }

  def unformatText(text: String, userList: Seq[SlackUserInfo]): String = {
    unescapeSlackHTMLEntities(
      unformatLinks(
        augmentUserIdsWithNames(text, userList)
      )
    )
  }

  def textContainsRawUserIds(text: String): Boolean = {
    """<@\w+>""".r.findFirstIn(text).isDefined
  }

}
