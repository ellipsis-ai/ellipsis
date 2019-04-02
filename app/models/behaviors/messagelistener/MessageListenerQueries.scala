package models.behaviors.messagelistener

import drivers.SlickPostgresDriver.api._
import json.Formatting.actionArgFormat
import models.accounts.user.{User, UserQueries, UsersTable}
import models.behaviors.ActionArg
import models.behaviors.behavior.BehaviorQueries
import play.api.libs.json.JsSuccess

object MessageListenerQueries {

  val all = TableQuery[MessageListenersTable]
  val allWithBehavior = all.join(BehaviorQueries.allWithGroup).on(_.behaviorId === _._1._1.id)
  val allWithUser = allWithBehavior.join(UserQueries.all).on(_._1.userId === _.id)

  type TupleType = ((RawMessageListener, BehaviorQueries.TupleType), User)
  type TableTupleType = ((MessageListenersTable, BehaviorQueries.TableTupleType), UsersTable)

  def tuple2Listener(tuple: TupleType): MessageListener = {
    val raw = tuple._1._1
    val behavior = BehaviorQueries.tuple2Behavior(tuple._1._2)
    val arguments: Seq[ActionArg] = raw.arguments.validate[Seq[ActionArg]] match {
      case JsSuccess(args, _) => args
      case _ => Seq()
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
      raw.createdAt
    )
  }

  def uncompiledAllForQuery(
                             teamId: Rep[String],
                             medium: Rep[String],
                             channel: Rep[String],
                             maybeThreadId: Rep[Option[String]]
                           ) = {
    allWithUser.
      filter { case(_, user) => user.teamId === teamId }.
      filter { case((listener, _), _) => listener.medium === medium }.
      filter { case((listener, _), _) => listener.channel === channel }.
      filter { case((listener, _), _) => listener.maybeThreadId.isEmpty || listener.maybeThreadId === maybeThreadId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
