package models.behaviors.messagelistener

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import models.accounts.user.{User, UserQueries, UsersTable}
import models.behaviors.behavior.BehaviorQueries
import play.api.libs.json.{JsObject, JsString, JsValue}

object MessageListenerQueries {

  val all = TableQuery[MessageListenersTable]
  val allWithBehavior = all.join(BehaviorQueries.allWithGroup).on(_.behaviorId === _._1._1.id)
  val allWithUser = allWithBehavior.join(UserQueries.all).on(_._1.userId === _.id)

  type TupleType = ((RawMessageListener, BehaviorQueries.TupleType), User)
  type TableTupleType = ((MessageListenersTable, BehaviorQueries.TableTupleType), UsersTable)

  def tuple2Listener(tuple: TupleType): MessageListener = {
    val raw = tuple._1._1
    val behavior = BehaviorQueries.tuple2Behavior(tuple._1._2)
    val arguments: Map[String, String] = raw.arguments match {
      case obj: JsObject => obj.value.map {
        case(k: String, v: JsString) => (k, v.value)
        case(k, v) => (k, v.toString)
      }.toMap
      case _ => Map[String, String]()
    }
    val user = tuple._2
    MessageListener(
      raw.id,
      behavior,
      arguments,
      raw.medium,
      raw.channel,
      raw.maybeThreadId,
      user,
      raw.isForCopilot,
      raw.isEnabled,
      raw.createdAt,
      raw.maybeLastCopilotActivityAt
    )
  }

  def uncompiledFindWithoutAccessCheckQuery(id: Rep[String]) = {
    allWithUser.filter { case((listener, _), _) => listener.id === id }
  }

  val findWithoutAccessCheckQuery = Compiled(uncompiledFindWithoutAccessCheckQuery _)

  def uncompiledFindForUserQuery(id: Rep[String], userId: Rep[String]) = {
    uncompiledFindWithoutAccessCheckQuery(id).filter { case((listener, _), _) => listener.userId === userId }
  }

  val findForUserQuery = Compiled(uncompiledFindForUserQuery _)

  def uncompiledFindForEnsureQuery(
                                    behaviorId: Rep[String],
                                    arguments: Rep[JsValue],
                                    userId: Rep[String],
                                    medium: Rep[String],
                                    channel: Rep[String],
                                    maybeThreadId: Rep[Option[String]],
                                    isForCopilot: Rep[Boolean]
                                  ) = {
    allWithUser.
      filter { case((listener, _), _) => listener.behaviorId === behaviorId }.
      filter { case((listener, _), _) => listener.arguments === arguments }.
      filter { case((listener, _), _) => listener.userId === userId }.
      filter { case((listener, _), _) => listener.medium === medium }.
      filter { case((listener, _), _) => listener.channel === channel }.
      filter { case((listener, _), _) => (listener.maybeThreadId.isEmpty && maybeThreadId.isEmpty) || listener.maybeThreadId === maybeThreadId }.
      filter { case((listener, _), _) => listener.isForCopilot === isForCopilot }
  }
  val findForEnsureQuery = Compiled(uncompiledFindForEnsureQuery _)

  def uncompiledAllForQuery(
                             teamId: Rep[String],
                             medium: Rep[String],
                             channel: Rep[String],
                             maybeThreadId: Rep[Option[String]]
                           ) = {
    allWithUser.
      filter { case(_, user) => user.teamId === teamId }.
      filter { case((listener, _), _) => listener.isEnabled }.
      filter { case((listener, _), _) => listener.medium === medium }.
      filter { case((listener, _), _) => listener.channel === channel }.
      filter { case((listener, _), _) => listener.maybeThreadId.isEmpty || listener.maybeThreadId === maybeThreadId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def uncompiledIsEnabledForUserBehavior(
                                          behaviorId: Rep[String],
                                          userId: Rep[String],
                                          medium: Rep[String],
                                          channel: Rep[String],
                                          maybeThreadId: Rep[Option[String]],
                                          isForCopilot: Rep[Boolean]
                                        ) = {
    all.
      filter { listener => listener.userId === userId }.
      filter { listener => listener.behaviorId === behaviorId }.
      filter { listener => listener.medium === medium }.
      filter { listener => listener.channel === channel }.
      filter { listener => (maybeThreadId.isEmpty && listener.maybeThreadId.isEmpty) || listener.maybeThreadId === maybeThreadId }.
      filter { listener => listener.isForCopilot === isForCopilot }.
      filter { listener => listener.isEnabled }.
      map(_.isEnabled)
  }

  val isEnabledForUserBehavior = Compiled(uncompiledIsEnabledForUserBehavior _)

  def uncompiledNoteCopilotActivityQuery(id: Rep[String]) = {
    all.filter(_.id === id).map(ea => (ea.isEnabled, ea.maybeLastCopilotActivityAt))
  }
  val noteCopilotActivityQuery = Compiled(uncompiledNoteCopilotActivityQuery _)

  def uncompiledIdleCopilotListenersQuery(idleCutoff: Rep[OffsetDateTime]) = {
    all.
      filter(_.isForCopilot).
      filter(_.isEnabled).
      filter(ea => ea.maybeLastCopilotActivityAt.isEmpty || ea.maybeLastCopilotActivityAt < idleCutoff).
      map(_.isEnabled)
  }
  val idleCopilotListenersQuery = Compiled(uncompiledIdleCopilotListenersQuery _)
}
