package models.bots.events

import models.bots.conversations.Conversation
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext

trait Context {

  def sendMessage(text: String, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit

  def maybeOngoingConversation: DBIO[Option[Conversation]]

}
