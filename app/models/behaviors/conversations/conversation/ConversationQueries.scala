package models.behaviors.conversations.conversation

import java.sql.Timestamp
import java.time.OffsetDateTime

import models.behaviors.conversations.InvokeBehaviorConversation
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries
import models.behaviors.events.EventType
import models.behaviors.triggers.TriggerQueries

object ConversationQueries {

  def all = TableQuery[ConversationsTable]
  def allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1.id)
  def allWithTrigger = allWithBehaviorVersion.joinLeft(TriggerQueries.allWithBehaviorVersion).on(_._1.maybeTriggerId === _._1.id)

  type TupleType = ((RawConversation, BehaviorVersionQueries.TupleType), Option[TriggerQueries.TupleType])

  def tuple2Conversation(tuple: TupleType): Conversation = {
    val raw = tuple._1._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._1._2)
    val maybeTrigger = tuple._2.map(TriggerQueries.tuple2Trigger)
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
      raw.maybeTeamIdForContext,
      raw.startedAt,
      raw.maybeLastInteractionAt,
      raw.state,
      raw.maybeScheduledMessageId,
      EventType.maybeFrom(raw.maybeOriginalEventType),
      raw.maybeParentId
    )
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    allWithTrigger.filter { case((convo, _), _) => convo.id === id }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllOngoingQueryFor(userIdForContext: Rep[String], context: Rep[String], teamId: Rep[String]) = {
    uncompiledAllPendingQuery.
      filter { case((convo, _), _) => convo.userIdForContext === userIdForContext }.
      filter { case((convo, _), _) => convo.context === context }.
      filter { case((_, (_, (b, _))), _) => b._2._1.teamId === teamId }.
      sortBy { case((convo, _), _) => convo.startedAt.desc }
  }
  val allOngoingQueryFor = Compiled(uncompiledAllOngoingQueryFor _)

  def uncompiledWithThreadIdQuery(threadId: Rep[String], userIdForContext: Rep[String], context: Rep[String]) = {
    allWithTrigger.
      filter { case((convo, _), _) => convo.userIdForContext === userIdForContext }.
      filter { case((convo, _), _) => convo.context === context }.
      filter { case((convo, _), _) => convo.maybeThreadId === threadId }
  }
  val withThreadIdQuery = Compiled(uncompiledWithThreadIdQuery _)

  // doing this instead of <> Conversation.DONE_STATE so db indexes get used
  def notDone(convo: ConversationsTable) = {
    convo.state === Conversation.NEW_STATE ||
      convo.state === Conversation.PENDING_STATE ||
      convo.state === Conversation.COLLECT_SIMPLE_TOKENS_STATE ||
      convo.state === Conversation.COLLECT_PARAM_VALUES_STATE
  }

  def uncompiledAllPendingQuery = {
    allWithTrigger.filter { case ((convo, _), _) => notDone(convo) }
  }

  def uncompiledAllForegroundQuery = {
    uncompiledAllPendingQuery.filterNot { case((convo, _), _) => convo.maybeThreadId.isDefined }
  }
  def allForegroundQuery = Compiled(uncompiledAllForegroundQuery)

  def uncompiledAllOngoingVersionIdsQuery(doneState: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(convo, _) => notDone(convo) }.
      map { case(_, ((bv, _), _)) => bv.groupVersionId }.
      distinct
  }
  val allOngoingVersionIdsQuery = Compiled(uncompiledAllOngoingVersionIdsQuery _)

  def uncompiledRawOldConversationsQuery(cutoff: Rep[OffsetDateTime], doneState: Rep[String]) = {
    all.
      filterNot(_.state === doneState).
      filter(_.startedAt < cutoff).
      filter(c => c.maybeLastInteractionAt.isEmpty || c.maybeLastInteractionAt < cutoff).
      map(_.state)
  }
  val cancelOldConversationsQuery = Compiled(uncompiledRawOldConversationsQuery _)

  val tableName: String = "conversations"
  val startedAtName: String = "started_at"
  val lastInteractionAtName: String = "last_interaction_at"

  def nextNeedingReminderIdQueryFor(windowStart: OffsetDateTime, windowEnd: OffsetDateTime): DBIO[Seq[String]] = {
    val startTs = Timestamp.from(windowStart.toInstant)
    val endTs = Timestamp.from(windowEnd.toInstant)
    sql"""
         SELECT id FROM #$tableName
         WHERE started_at >= ${startTs} AND state <> ${Conversation.DONE_STATE} AND
           ((#$lastInteractionAtName IS NOT NULL AND #$lastInteractionAtName < ${endTs}) OR
           (#$lastInteractionAtName IS NULL AND #$startedAtName < ${endTs}))
         ORDER BY id
         FOR UPDATE SKIP LOCKED
         LIMIT 1
       """.as[String]
  }

  def oldConversationCutoff: OffsetDateTime = OffsetDateTime.now.minusDays(1)

}
