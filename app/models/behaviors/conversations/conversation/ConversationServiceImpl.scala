package models.behaviors.conversations.conversation

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService, DefaultServices}

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
                            maybeLastInteractionAt: Option[OffsetDateTime],
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
  def maybeLastInteractionAt = column[Option[OffsetDateTime]]("last_interaction_at")
  def state = column[String]("state")
  def maybeScheduledMessageId = column[Option[String]]("scheduled_message_id")

  def * =
    (id, behaviorVersionId, maybeTriggerId, maybeTriggerMessage, conversationType, context, maybeChannel, maybeThreadId, userIdForContext, startedAt, maybeLastInteractionAt, state, maybeScheduledMessageId) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

class ConversationServiceImpl @Inject() (
                                          servicesProvider: Provider[DefaultServices]
                                         ) extends ConversationService {

  def services: DefaultServices = servicesProvider.get
  def dataService: DataService = services.dataService
  def lambdaService: AWSLambdaService = services.lambdaService
  def cache: CacheApi = services.cache
  def ws: WSClient = services.ws
  def configuration: Configuration = services.configuration

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

  def allOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Seq[Conversation]] = {
    val action = allOngoingQueryFor(userIdForContext, context).result.map { r =>
      r.map(tuple2Conversation)
    }.map { activeConvos =>
      maybeThreadId.map { threadId =>
        activeConvos.filter(_.maybeThreadId.contains(threadId))
      }.getOrElse {
        val withoutThreadId = activeConvos.filter(_.maybeThreadId.isEmpty)
        withoutThreadId.filter(_.maybeChannel == maybeChannel)
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

  def allNeedingReminder: Future[Seq[Conversation]] = {
    val reminderWindowStart = OffsetDateTime.now.minusHours(1)
    val reminderWindowEnd = OffsetDateTime.now.minusMinutes(30)
    val action = allNeedingReminderQuery(reminderWindowStart, reminderWindowEnd).result.map { r =>
      r.map(tuple2Conversation)
    }
    dataService.run(action)
  }

  def findOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Option[Conversation]] = {
    allOngoingFor(userIdForContext, context, maybeChannel: Option[String], maybeThreadId).map(_.headOption)
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

  def uncompiledTouchQuery(conversationId: Rep[String]) = all.filter(_.id === conversationId).map(_.maybeLastInteractionAt)
  val touchQuery = Compiled(uncompiledTouchQuery _)

  def touch(conversation: Conversation): Future[Conversation] = {
    val lastInteractionAt = OffsetDateTime.now
    val action = touchQuery(conversation.id).update(Some(lastInteractionAt)).map(_ => {})
    dataService.run(action).map { _ =>
      conversation.copyWithLastInteractionAt(lastInteractionAt)
    }
  }

  def background(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): Future[Unit] = {
    for {
      maybeEvent <- conversation.maybePlaceholderEvent(dataService)
      maybeLastTs <- maybeEvent.map { event =>
        val usernameString = if (includeUsername) { s"<@${event.userIdForContext}>: " } else { "" }
        event.sendMessage(
          s"""$usernameString$prompt You can continue the previous conversation in this thread:""".stripMargin,
          conversation.behaviorVersion.forcePrivateResponse,
          maybeShouldUnfurl = None,
          Some(conversation),
          maybeActions = None,
          dataService
        )
      }.getOrElse(Future.successful(None))
      _ <- maybeEvent.map { event =>
        val convoWithThreadId = conversation.copyWithMaybeThreadId(maybeLastTs)
        dataService.conversations.save(convoWithThreadId).flatMap { _ =>
          convoWithThreadId.respond(event, isReminding=false, services).map { result =>
            result.sendIn(None, dataService)
          }
        }
      }.getOrElse(Future.successful({}))
    } yield {}
  }

}
