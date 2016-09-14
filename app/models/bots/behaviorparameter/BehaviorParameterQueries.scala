package models.bots.behaviorparameter

import models.accounts.user.User
import models.bots.behavior.RawBehavior
import models.bots.behaviorversion.{BehaviorVersionQueries, RawBehaviorVersion}
import models.team.Team
import slick.driver.PostgresDriver.api._

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  type TupleType = (RawBehaviorParameter, ((RawBehaviorVersion, Option[User]), (RawBehavior, Team)))

  def tuple2Parameter(tuple: TupleType): BehaviorParameter = {
    val raw = tuple._1
    BehaviorParameter(
      raw.id,
      raw.name,
      raw.rank,
      BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2),
      raw.maybeQuestion,
      BehaviorParameterType.forName(raw.paramType)
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(param, ((behaviorVersion, user), team)) => behaviorVersion.id === behaviorVersionId}.
      sortBy { case(param, _) => param.rank.asc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

}
