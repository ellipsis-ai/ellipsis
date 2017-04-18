package models.behaviors.conversations.conversation

import java.time.OffsetDateTime

import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.triggers.messagetrigger.MessageTriggerQueries
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries

object ConversationQueries {

  def all = TableQuery[ConversationsTable]
  def allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1._1.id)
  def allWithTrigger = allWithBehaviorVersion.joinLeft(MessageTriggerQueries.allWithBehaviorVersion).on(_._1.maybeTriggerId === _._1.id)

  type TupleType = ((RawConversation, BehaviorVersionQueries.TupleType), Option[MessageTriggerQueries.TupleType])

  def tuple2Conversation(tuple: TupleType): Conversation = {
    val raw = tuple._1._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._1._2)
    val maybeTrigger = tuple._2.map(MessageTriggerQueries.tuple2Trigger)
    // When we have multiple kinds of conversations again, use conversationType to figure out which is which
    InvokeBehaviorConversation(
      raw.id,
      behaviorVersion,
      maybeTrigger,
      raw.maybeTriggerMessage,
      raw.context,
      raw.maybeChannel,
      raw.maybeThreadId,
      raw.userIdForContext,
      raw.startedAt,
      raw.maybeLastInteractionAt,
      raw.state,
      raw.maybeScheduledMessageId
    )
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    allWithTrigger.filter { case((convo, _), _) => convo.id === id }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllOngoingQueryFor(userIdForContext: Rep[String], context: Rep[String]) = {
    allWithTrigger.
      filter { case((convo, _), _) => convo.userIdForContext === userIdForContext }.
      filter { case((convo, _), _) => convo.context === context }.
      filterNot { case((convo, _), _) => convo.state === Conversation.DONE_STATE }
  }
  val allOngoingQueryFor = Compiled(uncompiledAllOngoingQueryFor _)

  def uncompiledAllPendingQuery = {
    val doneValue: Rep[String] = Conversation.DONE_STATE
    allWithTrigger.filterNot { case((convo, _), _) => convo.state === doneValue }
  }
  def allPendingQuery = Compiled(uncompiledAllPendingQuery)

  def uncompiledAllForegroundQuery = {
    uncompiledAllPendingQuery.filterNot { case((convo, _), _) => convo.maybeThreadId.isDefined }
  }
  def allForegroundQuery = Compiled(uncompiledAllForegroundQuery)

  def uncompiledAllNeedingReminderQuery(windowStart: Rep[OffsetDateTime], windowEnd: Rep[OffsetDateTime]) = {
    uncompiledAllPendingQuery.
      filter { case((convo, _), _) => convo.startedAt >= windowStart }.
      filter { case((convo, _), _) => convo.maybeLastInteractionAt.map(dt => dt < windowEnd).getOrElse(true) }
  }
  val allNeedingReminderQuery = Compiled(uncompiledAllNeedingReminderQuery _)

}
