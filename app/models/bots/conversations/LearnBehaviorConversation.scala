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
