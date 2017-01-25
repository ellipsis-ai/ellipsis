package models.behaviors.conversations.conversation

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import services.{AWSLambdaService, DataService}
import drivers.SlickPostgresDriver.api._
import models.behaviors.events.MessageEvent
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawConversation(
                            id: String,
                            triggerId: String,
                            conversationType: String,
                            context: String,
                            userIdForContext: String,
                            startedAt: OffsetDateTime,
                            state: String
                          )

class ConversationsTable(tag: Tag) extends Table[RawConversation](tag, "conversations") {

  def id = column[String]("id", O.PrimaryKey)
  def triggerId = column[String]("trigger_id")
  def conversationType = column[String]("conversation_type")
  def context = column[String]("context")
  def userIdForContext = column[String]("user_id_for_context")
  def startedAt = column[OffsetDateTime]("started_at")
  def state = column[String]("state")

  def * =
    (id, triggerId, conversationType, context, userIdForContext, startedAt, state) <>
      ((RawConversation.apply _).tupled, RawConversation.unapply _)
}

class ConversationServiceImpl @Inject() (
                                           dataServiceProvider: Provider[DataService],
                                           lambdaServiceProvider: Provider[AWSLambdaService],
                                           cache: CacheApi,
                                           configuration: Configuration,
                                           ws: WSClient
                                         ) extends ConversationService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

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

  def allOngoingFor(userIdForContext: String, context: String, isPrivateMessage: Boolean): Future[Seq[Conversation]] = {
    val action = allOngoingQueryFor(userIdForContext).result.map { r =>
      r.map(tuple2Conversation)
    }.map { activeConvos =>
      val requiresPrivate = if (isPrivateMessage) {
        activeConvos.filter(_.stateRequiresPrivateMessage)
      } else {
        Seq()
      }
      requiresPrivate ++ activeConvos.filterNot(_.stateRequiresPrivateMessage).filter(_.context == context)
    }
    dataService.run(action)
  }

  def findOngoingFor(userIdForContext: String, context: String, isPrivateMessage: Boolean): Future[Option[Conversation]] = {
    allOngoingFor(userIdForContext, context, isPrivateMessage).map(_.headOption)
  }

  def find(id: String): Future[Option[Conversation]] = {
    dataService.run(findQuery(id).result.map { r =>
      r.headOption.map(tuple2Conversation)
    })
  }

  def uncompiledStateQuery(conversationId: Rep[String]) = all.filter(_.id === conversationId).map(_.state)
  val stateQuery = Compiled(uncompiledStateQuery _)

  def cancel(conversation: Conversation): Future[Unit] = {
    val action = stateQuery(conversation.id).update(Conversation.DONE_STATE).map(_ => {})
    dataService.run(action)
  }

  def start(
             conversationId: String,
             teamId: String,
             event: MessageEvent
           ): Future[Unit] = {
    for {
      maybeConvo <- dataService.conversations.find(conversationId)
      maybeStarted <- maybeConvo.map { convo =>
        (if (convo.isPending) {
          convo.updateStateTo(Conversation.NEW_STATE, dataService)
        } else {
          Future.successful(convo)
        }).map(Some(_))
      }.getOrElse(Future.successful(None))
      _ <- maybeStarted.map { started =>
        started.resultFor(event, lambdaService, dataService, cache, ws, configuration).flatMap { result =>
          result.sendIn(None, Some(started))
        }
      }.getOrElse(Future.successful(None))
    } yield {}
  }

  def deleteAll(): Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

}
