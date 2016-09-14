package models.bots.conversations

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.bots._
import models.bots.behaviorversion.BehaviorVersion
import models.bots.events.MessageEvent
import models.bots.triggers.messagetrigger.{MessageTrigger, MessageTriggerQueries}
import org.joda.time.DateTime
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait Conversation {
  val id: String
  val trigger: MessageTrigger
  val behaviorVersion: BehaviorVersion = trigger.behaviorVersion
  val conversationType: String
  val context: String
  val userIdForContext: String
  val startedAt: DateTime
  val state: String

  def updateWith(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService): DBIO[Conversation]
  def respond(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService): DBIO[BehaviorResult]

  def resultFor(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService): DBIO[BehaviorResult] = {
    for {
      updatedConversation <- updateWith(event, lambdaService, dataService)
      result <- updatedConversation.respond(event, lambdaService, dataService)
    } yield result
  }

  def save: DBIO[Conversation] = ConversationQueries.save(this)

  def toRaw: RawConversation = {
    RawConversation(id, trigger.id, conversationType, context, userIdForContext, startedAt, state)
  }
}

case class RawConversation(
                            id: String,
                            triggerId: String,
                            conversationType: String,
                            context: String,
                            userIdForContext: String,
                            startedAt: DateTime,
                            state: String
                            )

class ConversationsTable(tag: Tag) extends Table[RawConversation](tag, "conversations") {

  def id = column[String]("id", O.PrimaryKey)
  def triggerId = column[String]("trigger_id")
  def conversationType = column[String]("conversation_type")
  def context = column[String]("context")
  def userIdForContext = column[String]("user_id_for_context")
  def startedAt = column[DateTime]("started_at")
  def state = column[String]("state")

  def * =
    (id, triggerId, conversationType, context, userIdForContext, startedAt, state) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

object ConversationQueries {
  def all = TableQuery[ConversationsTable]
  def allWithTrigger = all.join(MessageTriggerQueries.allWithBehaviorVersion).on(_.triggerId === _._1.id)

  type TupleType = (RawConversation, MessageTriggerQueries.TupleType)

  def tuple2Conversation(tuple: TupleType): Conversation = {
    val raw = tuple._1
    val trigger = MessageTriggerQueries.tuple2Trigger(tuple._2)
    // When we have multiple kinds of conversations again, use conversationType to figure out which is which
    InvokeBehaviorConversation(raw.id, trigger, raw.context, raw.userIdForContext, raw.startedAt, raw.state)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = allWithTrigger.filter(_._1.id === id)
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
        all += raw
      }
    }.map(_ => conversation)
  }

  def uncompiledFindWithoutStateQueryFor(userIdForContext: Rep[String], context: Rep[String], state: Rep[String]) = {
    allWithTrigger.
      filter { case(conversation, _) => conversation.userIdForContext === userIdForContext }.
      filter { case(conversation, _) => conversation.context === context}.
      filterNot { case(conversation, _) => conversation.state === state }
  }
  val findWithoutStateQueryFor = Compiled(uncompiledFindWithoutStateQueryFor _)

  def findOngoingFor(userIdForContext: String, context: String): DBIO[Option[Conversation]] = {
    findWithoutStateQueryFor(userIdForContext, context, Conversation.DONE_STATE).result.map(_.headOption.map(tuple2Conversation))
  }

}

object Conversation {
  val NEW_STATE = "new"
  val DONE_STATE = "done"

  val SLACK_CONTEXT = "slack"
  val API_CONTEXT = "api"

  val LEARN_BEHAVIOR = "learn_behavior"
  val INVOKE_BEHAVIOR = "invoke_behavior"
}
