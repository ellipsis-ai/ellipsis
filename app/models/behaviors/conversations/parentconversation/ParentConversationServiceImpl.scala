package models.behaviors.conversations.parentconversation

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.behaviors.conversations.conversation.Conversation
import services._
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class RawParentConversation(
                            id: String,
                            parentId: String,
                            paramId: String
                          )

class ParentConversationsTable(tag: Tag) extends Table[RawParentConversation](tag, "parent_conversations") {

  def id = column[String]("id", O.PrimaryKey)
  def parentId = column[String]("parent_id")
  def paramId = column[String]("param_id")

  def * = (id, parentId, paramId) <> ((RawParentConversation.apply _).tupled, RawParentConversation.unapply _)
}

class ParentConversationServiceImpl @Inject() (
                                          servicesProvider: Provider[DefaultServices],
                                          implicit val ec: ExecutionContext
                                        ) extends ParentConversationService {

  def services: DefaultServices = servicesProvider.get
  def dataService: DataService = services.dataService

  import ParentConversationQueries._

  def createAction(pc: NewParentConversation): DBIO[ParentConversation] = {
    val newInstance = ParentConversation(IDs.next, pc.parent, pc.param)
    (all += newInstance.toRaw).map(_ => newInstance)
  }

  def findAction(id: String): DBIO[Option[ParentConversation]] = {
    findQuery(id).result.map { r =>
      r.headOption.map(tuple2Parent)
    }
  }

  def ancestorsForAction(conversation: Conversation)(implicit ec: ExecutionContext): DBIO[List[Conversation]] = {
    conversation.maybeParentId.map { parentId =>
      findAction(parentId).flatMap { maybeParent =>
        maybeParent.map { p =>
          ancestorsForAction(p.parent).map { ancestors =>
            p.parent :: ancestors
          }
        }.getOrElse(DBIO.successful(List()))
      }
    }.getOrElse(DBIO.successful(List()))
  }

  def maybeForAction(conversation: Conversation): DBIO[Option[ParentConversation]] = {
    conversation.maybeParentId.map { parentId =>
      findAction(parentId)
    }.getOrElse(DBIO.successful(None))
  }

}
