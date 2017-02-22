package models.behaviors.conversations.conversation

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawConversation(
                            id: String,
                            behaviorVersionId: String,
                            maybeTriggerId: Option[String],
                            maybeTriggerMessage: Option[String],
                            conversationType: String,
                            context: String,
                            maybeChannel: Option[String],
                            maybeThreadId: Option[String],
                            userIdForContext: String,
                            startedAt: OffsetDateTime,
                            state: String,
                            maybeScheduledMessageId: Option[String]
                          )

class ConversationsTable(tag: Tag) extends Table[RawConversation](tag, "conversations") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeTriggerId = column[Option[String]]("trigger_id")
  def maybeTriggerMessage = column[Option[String]]("trigger_message")
  def conversationType = column[String]("conversation_type")
  def context = column[String]("context")
  def maybeChannel = column[Option[String]]("channel")
  def maybeThreadId = column[Option[String]]("thread_id")
  def userIdForContext = column[String]("user_id_for_context")
  def startedAt = column[OffsetDateTime]("started_at")
  def state = column[String]("state")
  def maybeScheduledMessageId = column[Option[String]]("scheduled_message_id")

  def * =
    (id, behaviorVersionId, maybeTriggerId, maybeTriggerMessage, conversationType, context, maybeChannel, maybeThreadId, userIdForContext, startedAt, state, maybeScheduledMessageId) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

class ConversationServiceImpl @Inject() (
                                           dataServiceProvider: Provider[DataService]
                                         ) extends ConversationService {

  def dataService = dataServiceProvider.get

  import ConversationQueries._

  def save(conversation: Conversation): Future[Conversation] = {
    val query = all.filter(_.id === conversation.id)
    val raw = conversation.toRaw
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => conversation)
    dataService.run(action)
  }

  def allOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String], isPrivateMessage: Boolean): Future[Seq[Conversation]] = {
    val action = allOngoingQueryFor(userIdForContext, context).result.map { r =>
      r.map(tuple2Conversation)
    }.map { activeConvos =>
      maybeThreadId.map { threadId =>
        activeConvos.filter(_.maybeThreadId.contains(threadId))
      }.getOrElse {
        val withoutThreadId = activeConvos.filter(_.maybeThreadId.isEmpty)
        val withMatchingChannel = withoutThreadId.filter(_.maybeChannel == maybeChannel)
        val matchingBecausePrivate = if (isPrivateMessage) {
          withoutThreadId.filter(_.stateRequiresPrivateMessage)
        } else {
          Seq()
        }
        withMatchingChannel ++ matchingBecausePrivate
      }

    }
    dataService.run(action)
  }

  def allForeground: Future[Seq[Conversation]] = {
    val action = allForegroundQuery.result.map { r =>
      r.map(tuple2Conversation)
    }
    dataService.run(action)
  }

  def findOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String], isPrivateMessage: Boolean): Future[Option[Conversation]] = {
    allOngoingFor(userIdForContext, context, maybeChannel: Option[String], maybeThreadId, isPrivateMessage).map(_.headOption)
  }

  def uncompiledCancelQuery(conversationId: Rep[String]) = all.filter(_.id === conversationId).map(_.state)
  val cancelQuery = Compiled(uncompiledCancelQuery _)

  def cancel(conversation: Conversation): Future[Unit] = {
    val action = cancelQuery(conversation.id).update(Conversation.DONE_STATE).map(_ => {})
    dataService.run(action)
  }

  def deleteAll(): Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

  def find(id: String): Future[Option[Conversation]] = {
    val action = findQueryFor(id).result.map { r =>
      r.headOption.map(tuple2Conversation)
    }
    dataService.run(action)
  }

  def isDone(id: String): Future[Boolean] = {
    find(id).map { maybeConversation =>
      maybeConversation.exists(_.state == Conversation.DONE_STATE)
    }
  }

}
