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
    InvokeBehaviorConversation(raw.id, trigger, raw.triggerMessage, raw.context, raw.maybeChannel, raw.maybeThreadId, raw.userIdForContext, raw.startedAt, raw.state, raw.maybeScheduledMessageId)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = allWithTrigger.filter(_._1.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllOngoingQueryFor(userIdForContext: Rep[String], context: Rep[String]) = {
    allWithTrigger.
      filter { case(convo, _) => convo.userIdForContext === userIdForContext }.
      filter { case(convo, _) => convo.context === context }.
      filterNot { case(convo, _) => convo.state === Conversation.DONE_STATE }
  }
  val allOngoingQueryFor = Compiled(uncompiledAllOngoingQueryFor _)

  def uncompiledAllForegroundQuery = {
    val doneValue: Rep[String] = Conversation.DONE_STATE
    allWithTrigger.
      filterNot { case(conversation, _) => conversation.state === doneValue }.
      filterNot { case(conversation, _) => conversation.maybeThreadId.isDefined }
  }
  def allForegroundQuery = Compiled(uncompiledAllForegroundQuery)

}
