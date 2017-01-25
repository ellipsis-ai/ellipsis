package models.behaviors.conversations.conversation

import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.triggers.messagetrigger.MessageTriggerQueries
import drivers.SlickPostgresDriver.api._

object ConversationQueries {

  def all = TableQuery[ConversationsTable]
  def allWithTrigger = all.join(MessageTriggerQueries.allWithBehaviorVersion).on(_.triggerId === _._1.id)

  type TupleType = (RawConversation, MessageTriggerQueries.TupleType)

  def tuple2Conversation(tuple: TupleType): Conversation = {
    val raw = tuple._1
    val trigger = MessageTriggerQueries.tuple2Trigger(tuple._2)
    // When we have multiple kinds of conversations again, use conversationType to figure out which is which
    InvokeBehaviorConversation(raw.id, trigger, raw.context, raw.userIdForContext, raw.startedAt, raw.state, justConfirmedReady = false)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = allWithTrigger.filter(_._1.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllWithoutStateQueryFor(userIdForContext: Rep[String], state: Rep[String]) = {
    allWithTrigger.
      filter { case(conversation, _) => conversation.userIdForContext === userIdForContext }.
      filterNot { case(conversation, _) => conversation.state === state }
  }
  val allWithoutStateQueryFor = Compiled(uncompiledAllWithoutStateQueryFor _)

  def uncompiledAllOngoingQueryFor(userIdForContext: Rep[String]) = {
    allWithTrigger.
      filter { case(conversation, _) => conversation.userIdForContext === userIdForContext }.
      filterNot { case(conversation, _) => conversation.state === Conversation.DONE_STATE }.
      filterNot { case(conversation, _) => conversation.state === Conversation.PENDING_STATE }
  }
  val allOngoingQueryFor = Compiled(uncompiledAllOngoingQueryFor _)

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTrigger.filter { case(conversation, _) => conversation.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

}
