package models.behaviors.conversations.conversation

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.BotResultService
import models.behaviors.conversations.ConversationServices
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, CacheService, DataService, SlackEventService}
import slick.dbio.DBIO

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

class ConversationsTable(tag: Tag) extends Table[RawConversation](tag, ConversationQueries.tableName) {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeTriggerId = column[Option[String]]("trigger_id")
  def maybeTriggerMessage = column[Option[String]]("trigger_message")
  def conversationType = column[String]("conversation_type")
  def context = column[String]("context")
  def maybeChannel = column[Option[String]]("channel")
  def maybeThreadId = column[Option[String]]("thread_id")
  def userIdForContext = column[String]("user_id_for_context")
  def startedAt = column[OffsetDateTime](ConversationQueries.startedAtName)
  def maybeLastInteractionAt = column[Option[OffsetDateTime]](ConversationQueries.lastInteractionAtName)
  def state = column[String]("state")
  def maybeScheduledMessageId = column[Option[String]]("scheduled_message_id")

  def * =
    (id, behaviorVersionId, maybeTriggerId, maybeTriggerMessage, conversationType, context, maybeChannel, maybeThreadId, userIdForContext, startedAt, maybeLastInteractionAt, state, maybeScheduledMessageId) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

class ConversationServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService],
                                          slackEventServiceProvier: Provider[SlackEventService],
                                          lambdaServiceProvider: Provider[AWSLambdaService],
                                          cacheServiceProvider: Provider[CacheService],
                                          wsProvider: Provider[WSClient],
                                          configurationProvider: Provider[Configuration],
                                          botResultServiceProvider: Provider[BotResultService],
                                          actorSystem: ActorSystem
                                         ) extends ConversationService {

  def dataService: DataService = dataServiceProvider.get
  def slackEventService: SlackEventService = slackEventServiceProvier.get
  def lambdaService: AWSLambdaService = lambdaServiceProvider.get
  def cacheService: CacheService = cacheServiceProvider.get
  def ws: WSClient = wsProvider.get
  def configuration: Configuration = configurationProvider.get
  def botResultService: BotResultService = botResultServiceProvider.get
  def services: ConversationServices = ConversationServices(dataService, lambdaService, slackEventService, cacheService, configuration, ws, actorSystem)

  import ConversationQueries._

  def saveAction(conversation: Conversation): DBIO[Conversation] = {
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

  def save(conversation: Conversation): Future[Conversation] = {
    dataService.run(saveAction(conversation))
  }

  def allOngoingForAction(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): DBIO[Seq[Conversation]] = {
    allOngoingQueryFor(userIdForContext, context).result.map { r =>
      r.map(tuple2Conversation)
    }.map { activeConvos =>
      maybeThreadId.map { threadId =>
        activeConvos.filter(_.maybeThreadId.contains(threadId))
      }.getOrElse {
        val withoutThreadId = activeConvos.filter(_.maybeThreadId.isEmpty)
        withoutThreadId.filter(_.maybeChannel == maybeChannel)
      }
    }
  }

  def allOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Seq[Conversation]] = {
    dataService.run(allOngoingForAction(userIdForContext, context, maybeChannel, maybeThreadId))
  }

  def allForeground: Future[Seq[Conversation]] = {
    val action = allForegroundQuery.result.map { r =>
      r.map(tuple2Conversation)
    }
    dataService.run(action)
  }

  def maybeNextNeedingReminderAction(when: OffsetDateTime): DBIO[Option[Conversation]] = {
    val reminderWindowStart = when.minusHours(1)
    val reminderWindowEnd = when.minusMinutes(30)
    for {
      maybeId <- nextNeedingReminderIdQueryFor(reminderWindowStart, reminderWindowEnd).map(_.headOption)
      maybeConvo <- maybeId.map(findAction).getOrElse(DBIO.successful(None))
    } yield maybeConvo
  }

  def findOngoingFor(userIdForContext: String, context: String, maybeChannel: Option[String], maybeThreadId: Option[String]): Future[Option[Conversation]] = {
    allOngoingFor(userIdForContext, context, maybeChannel: Option[String], maybeThreadId).map(_.headOption)
  }

  def uncompiledCancelQuery(conversationId: Rep[String]) = all.filter(_.id === conversationId).map(_.state)
  val cancelQuery = Compiled(uncompiledCancelQuery _)

  def cancelAction(conversation: Conversation): DBIO[Unit] = {
    cancelQuery(conversation.id).update(Conversation.DONE_STATE).map(_ => {})
  }

  def cancel(conversation: Conversation): Future[Unit] = {
    dataService.run(cancelAction(conversation))
  }

  def deleteAll(): Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

  def findAction(id: String): DBIO[Option[Conversation]] = {
    findQueryFor(id).result.map { r =>
      r.headOption.map(tuple2Conversation)
    }
  }

  def find(id: String): Future[Option[Conversation]] = {
    dataService.run(findAction(id))
  }

  def isDone(id: String): Future[Boolean] = {
    find(id).map { maybeConversation =>
      maybeConversation.exists(_.state == Conversation.DONE_STATE)
    }
  }

  def uncompiledTouchQuery(conversationId: Rep[String]) = all.filter(_.id === conversationId).map(_.maybeLastInteractionAt)
  val touchQuery = Compiled(uncompiledTouchQuery _)

  def touchAction(conversation: Conversation): DBIO[Conversation] = {
    val lastInteractionAt = OffsetDateTime.now
    touchQuery(conversation.id).update(Some(lastInteractionAt)).map { _ =>
      conversation.copyWithLastInteractionAt(lastInteractionAt)
    }
  }

  def touch(conversation: Conversation): Future[Conversation] = {
    dataService.run(touchAction(conversation))
  }

  def backgroundAction(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): DBIO[Unit] = {
    for {
      maybeEvent <- conversation.maybePlaceholderEventAction(services)
      maybeLastTs <- maybeEvent.map { event =>
        DBIO.from(event.sendMessage(
          interruptionPromptFor(event, prompt, includeUsername),
          conversation.behaviorVersion.forcePrivateResponse,
          maybeShouldUnfurl = None,
          Some(conversation),
          maybeActions = None
        ))
      }.getOrElse(DBIO.successful(None))
      _ <- maybeEvent.map { event =>
        val convoWithThreadId = conversation.copyWithMaybeThreadId(maybeLastTs)
        dataService.conversations.saveAction(convoWithThreadId).flatMap { _ =>
          convoWithThreadId.respondAction(event, isReminding=false, services).map { result =>
            botResultService.sendInAction(result, None)
          }
        }
      }.getOrElse(DBIO.successful({}))
    } yield {}
  }

  def background(conversation: Conversation, prompt: String, includeUsername: Boolean)(implicit actorSystem: ActorSystem): Future[Unit] = {
    dataService.run(backgroundAction(conversation, prompt, includeUsername))
  }

}
