package models.bots

import models.bots.conversations.Conversation
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext

class TimeContext extends Context {

  def maybeOngoingConversation: DBIO[Option[Conversation]] = DBIO.successful(None)

  def sendMessage(text: String)(implicit ec: ExecutionContext): Unit = {}

}
