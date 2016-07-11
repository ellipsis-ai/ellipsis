package models.bots

import java.util

import models.bots.conversations.Conversation
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext

trait MessageContext extends Context {
  val fullMessageText: String
  val relevantMessageText: String
  val includesBotMention: Boolean

  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit

  def teachMeLinkFor(lambdaService: AWSLambdaService): String = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.newBehavior(Some(teamId))
      s"$baseUrl$path"
    }.get
    s"[teach me something new]($newBehaviorLink)"
  }

  def sendIDontKnowHowToRespondMessageFor(lambdaService: AWSLambdaService)(implicit ec: ExecutionContext): Unit = {
    sendMessage(s"""
                   |I don't know how to respond to `$fullMessageText`
                   |
                   |Type `@ellipsis: help` to see what I can do or ${teachMeLinkFor(lambdaService)}
    """.stripMargin)
  }

  def recentMessages: DBIO[Seq[String]] = DBIO.successful(Seq())
  def maybeOngoingConversation: DBIO[Option[Conversation]]

  val name: String
  def userIdForContext: String
  val teamId: String
  val isResponseExpected: Boolean

  val COMMONMARK_EXTENSIONS = util.Arrays.asList(StrikethroughExtension.create, AutolinkExtension.create)

  def commonmarkParser = {
    Parser.builder().extensions(COMMONMARK_EXTENSIONS).build()
  }

  def commonmarkNodeFor(text: String): Node = {
    val node = commonmarkParser.parse(text)
    node.accept(new CommonmarkVisitor())
    node
  }
}
