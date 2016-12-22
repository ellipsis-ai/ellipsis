package models.behaviors.conversations.conversation

import javax.inject.Inject

import com.google.inject.Provider
import org.joda.time.DateTime
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  def allOngoingFor(userIdForContext: String, context: String, isPrivateMessage: Boolean): Future[Seq[Conversation]] = {
    val action = allWithoutStateQueryFor(userIdForContext, Conversation.DONE_STATE).result.map { r =>
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

  def uncompiledCancelQuery(conversationId: Rep[String]) = all.filter(_.id === conversationId).map(_.state)
  val cancelQuery = Compiled(uncompiledCancelQuery _)

  def cancel(conversation: Conversation): Future[Unit] = {
    val action = cancelQuery(conversation.id).update(Conversation.DONE_STATE).map(_ => {})
    dataService.run(action)
  }

  def deleteAll(): Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

}
