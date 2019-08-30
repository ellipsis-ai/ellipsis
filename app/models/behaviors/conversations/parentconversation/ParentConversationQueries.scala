package models.behaviors.conversations.parentconversation

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorparameter.BehaviorParameterQueries
import models.behaviors.conversations.conversation.ConversationQueries

object ParentConversationQueries {

  val all = TableQuery[ParentConversationsTable]
  val allWithConversation = all.join(ConversationQueries.allWithTrigger).on(_.parentId === _._1._1.id)
  val allWithParam = allWithConversation.join(BehaviorParameterQueries.allWithInput).on(_._1.paramId === _._1.id)

  type TupleType = ((RawParentConversation, ConversationQueries.TupleType), BehaviorParameterQueries.TupleType)

  def tuple2Parent(tuple: TupleType): ParentConversation = {
    ParentConversation(
      tuple._1._1.id,
      ConversationQueries.tuple2Conversation(tuple._1._2),
      BehaviorParameterQueries.tuple2Parameter(tuple._2)
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithParam.filter { case((parent, _), _) => parent.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
