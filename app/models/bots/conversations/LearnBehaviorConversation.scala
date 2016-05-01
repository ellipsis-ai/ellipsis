package models.bots.conversations

import models.IDs
import models.bots.Behavior
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class LearnBehaviorConversation(
                         id: String,
                         behavior: Behavior,
                         context: String, // Slack, etc
                         userIdForContext: String, // id for Slack, etc user
                         startedAt: DateTime,
                         isEnded: Boolean
                         ) extends Conversation {

  val conversationType = "learn_behavior"

  def replyFor(message: String): DBIO[String] = {
    val reply = if ("""learn\\s*$$""".r.findFirstMatchIn(message).nonEmpty) {
      "OK, teach me."
    } else {
      "I'm in a conversation!"
    }
    DBIO.successful(reply)
  }
}

object LearnBehaviorConversation {
  def createFor(
                 behavior: Behavior,
                 context: String,
                 userIdForContext: String
                 ): DBIO[LearnBehaviorConversation] = {
    val newInstance = LearnBehaviorConversation(IDs.next, behavior, context, userIdForContext, DateTime.now, false)
    newInstance.save.map(_ => newInstance)
  }
}
