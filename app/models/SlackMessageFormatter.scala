package models

import java.util

import models.bots.CommonmarkVisitor
import models.bots.templates.SlackRenderer
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import slack.rtm.SlackRtmClient

case class SlackMessageFormatter(client: SlackRtmClient) {

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

}
