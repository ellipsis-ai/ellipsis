package models.bots

import models.bots.conversations.Conversation
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext

trait MessageContext {
  val relevantMessageText: String
  val includesBotMention: Boolean
  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit
  def sendIDontKnowHowToRespondMessageFor(lambdaService: AWSLambdaService)(implicit ec: ExecutionContext): Unit
  def recentMessages: DBIO[Seq[String]]
  def maybeOngoingConversation: DBIO[Option[Conversation]]
  def teachMeLinkFor(lambdaService: AWSLambdaService): String
  val name: String
  def userIdForContext: String
  val teamId: String
  val isResponseExpected: Boolean
}
