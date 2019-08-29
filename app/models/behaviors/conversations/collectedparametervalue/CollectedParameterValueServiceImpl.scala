package models.behaviors.conversations.collectedparametervalue

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterQueries}
import models.behaviors.conversations.conversation.{Conversation, ConversationQueries}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class CollectedParameterValue(parameterId: String, conversationId: String, valueString: String)

class CollectedParameterValuesTable(tag: Tag) extends Table[CollectedParameterValue](tag, "collected_parameter_values") {

  def parameterId = column[String]("parameter_id")
  def conversationId = column[String]("conversation_id")
  def valueString = column[String]("value_string")

  def * = (parameterId, conversationId, valueString) <> ((CollectedParameterValue.apply _).tupled, CollectedParameterValue.unapply _)
}

class CollectedParameterValueServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService],
                                                     implicit val ec: ExecutionContext
                                                   ) extends CollectedParameterValueService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[CollectedParameterValuesTable]
  val joined =
    all.
      join(BehaviorParameterQueries.allWithBehaviorVersion).on(_.parameterId === _._1._1.id).
      join(ConversationQueries.allWithTrigger).on(_._1.conversationId === _._1._1.id)

  def uncompiledAllForQuery(conversationId: Rep[String]) = {
    all.filter(_.conversationId === conversationId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allForAction(conversation: Conversation): DBIO[Seq[CollectedParameterValue]] = {
    allForQuery(conversation.id).result
  }

  def uncompiledFindQuery(parameterId: Rep[String], conversationId: Rep[String]) = {
    all.filter(_.parameterId === parameterId).filter(_.conversationId === conversationId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def ensureForAction(
                 parameter: BehaviorParameter,
                 conversation: Conversation,
                 valueString: String
               ): DBIO[CollectedParameterValue] = {
    val instance = CollectedParameterValue(parameter.id, conversation.id, valueString)
    val query = findQuery(instance.parameterId, instance.conversationId)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(instance)
      }.getOrElse {
        all += instance
      }
    }.map(_ => instance)
  }

  def ensureFor(
                 parameter: BehaviorParameter,
                 conversation: Conversation,
                 valueString: String
               ): Future[CollectedParameterValue] = {
    dataService.run(ensureForAction(parameter, conversation, valueString))
  }

  def deleteForAction(parameterId: String, conversation: Conversation): DBIO[Unit] = {
    findQuery(parameterId, conversation.id).delete.map(_ => {})
  }

  def deleteFor(parameterId: String, conversation: Conversation): Future[Unit] = {
    dataService.run(deleteForAction(parameterId, conversation))
  }

  def deleteAll(): Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

}
