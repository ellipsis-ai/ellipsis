package models.bots.conversations.collectedparametervalue

import javax.inject.Inject

import com.google.inject.Provider
import models.bots.behaviorparameter.BehaviorParameterQueries
import models.bots.conversations.conversation.{Conversation, ConversationQueries}
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawCollectedParameterValue(parameterId: String, conversationId: String, valueString: String)

class CollectedParameterValuesTable(tag: Tag) extends Table[RawCollectedParameterValue](tag, "collected_parameter_values") {

  def parameterId = column[String]("parameter_id")
  def conversationId = column[String]("conversation_id")
  def valueString = column[String]("value_string")

  def * = (parameterId, conversationId, valueString) <> ((RawCollectedParameterValue.apply _).tupled, RawCollectedParameterValue.unapply _)
}

class CollectedParameterValueServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService]
                                                   ) extends CollectedParameterValueService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[CollectedParameterValuesTable]
  val joined =
    all.
      join(BehaviorParameterQueries.allWithBehaviorVersion).on(_.parameterId === _._1.id).
      join(ConversationQueries.allWithTrigger).on(_._1.conversationId === _._1.id)

  type TupleType = ((RawCollectedParameterValue, BehaviorParameterQueries.TupleType), ConversationQueries.TupleType)

  def tuple2ParameterValue(tuple: TupleType): CollectedParameterValue = {
    val param = BehaviorParameterQueries.tuple2Parameter(tuple._1._2)
    val conversation = ConversationQueries.tuple2Conversation(tuple._2)
    CollectedParameterValue(param, conversation, tuple._1._1.valueString)
  }

  def uncompiledAllForQuery(conversationId: Rep[String]) = {
    joined.filter { case((value, _), _) => value.conversationId === conversationId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(conversation: Conversation): Future[Seq[CollectedParameterValue]] = {
    val action = allForQuery(conversation.id).result.map(_.map(tuple2ParameterValue))
    dataService.run(action)
  }

  def save(value: CollectedParameterValue): Future[CollectedParameterValue] = {
    val raw = value.toRaw
    val query = all.filter(_.parameterId === raw.parameterId).filter(_.conversationId === raw.conversationId)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => value)
    dataService.run(action)
  }
}
