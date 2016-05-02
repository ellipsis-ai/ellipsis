package models.bots.conversations

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.Team
import models.bots.{RawBehavior, BehaviorQueries, Behavior}
import org.joda.time.DateTime
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait Conversation {
  val id: String
  val behavior: Behavior
  val conversationType: String
  val context: String
  val userIdForContext: String
  val startedAt: DateTime
  val state: String

  def replyStringFor(message: String): DBIO[String]
  def replyFor(message: String, lambdaService: AWSLambdaService): DBIO[String]

  def save: DBIO[Conversation] = ConversationQueries.save(this)

  def toRaw: RawConversation = {
    RawConversation(id, behavior.id, conversationType, context, userIdForContext, startedAt, state)
  }
}

case class RawConversation(
                            id: String,
                            behaviorId: String,
                            conversationType: String,
                            context: String,
                            userIdForContext: String,
                            startedAt: DateTime,
                            state: String
                            )

class ConversationsTable(tag: Tag) extends Table[RawConversation](tag, "conversations") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def conversationType = column[String]("conversation_type")
  def context = column[String]("context")
  def userIdForContext = column[String]("user_id_for_context")
  def startedAt = column[DateTime]("started_at")
  def state = column[String]("state")

  def * =
    (id, behaviorId, conversationType, context, userIdForContext, startedAt, state) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

object ConversationQueries {
  def all = TableQuery[ConversationsTable]
  def allWithBehavior = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  def tuple2Conversation(tuple: (RawConversation, (RawBehavior, Team))): Conversation = {
    val raw = tuple._1
    //    if (raw.conversationType == LEARNING_BEHAVIOR) {
    LearnBehaviorConversation(raw.id, BehaviorQueries.tuple2Behavior(tuple._2), raw.context, raw.userIdForContext, raw.startedAt, raw.state)
    //    }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = allWithBehavior.filter(_._1.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[Conversation]] = {
    findQueryFor(id).result.map(_.headOption.map(tuple2Conversation))
  }

  def save(conversation: Conversation): DBIO[Conversation] = {
    val query = all.filter(_.id === conversation.id)
    val raw = conversation.toRaw
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += conversation.toRaw
      }
    }.map(_ => conversation)
  }

  def uncompiledFindOngoingQueryFor(userIdForContext: Rep[String], context: Rep[String]) = {
    allWithBehavior.
      filter { case(conversation, _) => conversation.userIdForContext === userIdForContext }.
      filter { case(conversation, _) => conversation.context === context}.
      filterNot { case(conversation, _) => conversation.state === LearnBehaviorConversation.DONE_STATE }
  }
  val findOngoingQueryFor = Compiled(uncompiledFindOngoingQueryFor _)

  def findOngoingFor(userIdForContext: String, context: String): DBIO[Option[Conversation]] = {
    findOngoingQueryFor(userIdForContext, context).result.map(_.headOption.map(tuple2Conversation))
  }

  val SLACK_CONTEXT = "slack"

  val LEARNING_BEHAVIOR = "learning_behavior"
}
