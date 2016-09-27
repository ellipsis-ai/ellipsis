package models.behaviors.conversations.collectedparametervalue

import javax.inject.Inject

import com.google.inject.Provider
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterQueries}
import models.behaviors.conversations.conversation.{Conversation, ConversationQueries}
import play.api.cache.CacheApi
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
                                                     dataServiceProvider: Provider[DataService],
                                                     cacheProvider: Provider[CacheApi]
                                                   ) extends CollectedParameterValueService {

  def dataService = dataServiceProvider.get
  def cache = cacheProvider.get

  val all = TableQuery[CollectedParameterValuesTable]
  val joined =
    all.
      join(BehaviorParameterQueries.joined).on(_.parameterId === _._1._1.id).
      join(ConversationQueries.allWithTrigger).on(_._1.conversationId === _._1.id)

  type TupleType = ((RawCollectedParameterValue, BehaviorParameterQueries.TupleType), ConversationQueries.TupleType)

  def tuple2ParameterValue(tuple: TupleType): Future[CollectedParameterValue] = {
    val param = BehaviorParameterQueries.tuple2Parameter(tuple._1._2)
    val conversation = ConversationQueries.tuple2Conversation(tuple._2)
    val valueString = tuple._1._1.valueString
    param.paramType.isValid(valueString, Some(conversation), param, cache).map { isValid =>
      CollectedParameterValue(param, conversation, valueString, isValid)
    }
  }

  def uncompiledAllForQuery(conversationId: Rep[String]) = {
    joined.filter { case((value, _), _) => value.conversationId === conversationId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(conversation: Conversation): Future[Seq[CollectedParameterValue]] = {
    dataService.run(allForQuery(conversation.id).result).flatMap { tuples =>
      Future.sequence(tuples.map(tuple2ParameterValue))
    }
  }

  def uncompiledFindQuery(parameterId: Rep[String], conversationId: Rep[String]) = {
    joined.
      filter { case((collected, _), _) => collected.parameterId === parameterId }.
      filter { case((collected, _), _) => collected.conversationId === conversationId }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(parameter: BehaviorParameter, conversation: Conversation): Future[Option[CollectedParameterValue]] = {
    dataService.run(findQuery(parameter.id, conversation.id).result).flatMap { r =>
      r.headOption.map { tuple =>
        tuple2ParameterValue(tuple).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  def ensureFor(
                 parameter: BehaviorParameter,
                 conversation: Conversation,
                 valueString: String
               ): Future[CollectedParameterValue] = {
    val raw = RawCollectedParameterValue(parameter.id, conversation.id, valueString)
    val query = all.filter(_.parameterId === raw.parameterId).filter(_.conversationId === raw.conversationId)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }
    dataService.run(action).flatMap { _ =>
      find(parameter, conversation).map(_.get)
    }
  }

}
