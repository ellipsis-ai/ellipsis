package models.bots.conversations

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.Team
import models.bots.{RawBehavior, BehaviorQueries, Behavior}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait Conversation {
  val id: String
  val behavior: Behavior
  val conversationType: String
  val context: String
  val userIdForContext: String
  val startedAt: DateTime
  val isEnded: Boolean

  def save: DBIO[Conversation] = ConversationQueries.save(this)

  def toRaw: RawConversation = {
    RawConversation(id, behavior.id, conversationType, context, userIdForContext, startedAt, isEnded)
  }
}

case class RawConversation(
                            id: String,
                            behaviorId: String,
                            conversationType: String,
                            context: String,
                            userIdForContext: String,
                            startedAt: DateTime,
                            isEnded: Boolean
                            )

class ConversationsTable(tag: Tag) extends Table[RawConversation](tag, "conversations") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def conversationType = column[String]("conversation_type")
  def context = column[String]("context")
  def userIdForContext = column[String]("user_id_for_context")
  def startedAt = column[DateTime]("started_at")
  def isEnded = column[Boolean]("is_ended")

  def * =
    (id, behaviorId, conversationType, context, userIdForContext, startedAt, isEnded) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

object ConversationQueries {
  def all = TableQuery[ConversationsTable]
  def allWithBehavior = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  def tuple2Conversation(tuple: (RawConversation, (RawBehavior, Team))): Conversation = {
    val raw = tuple._1
    //    if (raw.conversationType == LEARNING_BEHAVIOR) {
    LearnBehaviorConversation(raw.id, BehaviorQueries.tuple2Behavior(tuple._2), raw.context, raw.userIdForContext, raw.startedAt, raw.isEnded)
    //    }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = allWithBehavior.filter(_._1.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[Conversation]] = {
    findQueryFor(id).result.map(_.headOption.map(tuple2Conversation))
  }

  def save(conversation: Conversation): DBIO[Conversation] = {
    (all += conversation.toRaw).map(_ => conversation)
  }

  def uncompiledFindOngoingQueryFor(userIdForContext: Rep[String], context: Rep[String]) = {
    allWithBehavior.
      filter { case(conversation, _) => conversation.userIdForContext === userIdForContext }.
      filter { case(conversation, _) => conversation.context === context}
  }
  val findOngoingQueryFor = Compiled(uncompiledFindOngoingQueryFor _)

  def findOngoingFor(userIdForContext: String, context: String): DBIO[Option[Conversation]] = {
    findOngoingQueryFor(userIdForContext, context).result.map(_.headOption.map(tuple2Conversation))
  }

  val SLACK_CONTEXT = "slack"

  val LEARNING_BEHAVIOR = "learning_behavior"
}
