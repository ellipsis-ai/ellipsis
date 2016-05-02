package models.bots

import models.{IDs, Team}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorParameter(
                            id: String,
                            name: String,
                            behavior: Behavior,
                            maybeQuestion: Option[String],
                            maybeParamType: Option[String]
                              ) {

  def isComplete: Boolean = {
    maybeQuestion.isDefined
  }

  def save: DBIO[BehaviorParameter] = BehaviorParameterQueries.save(this)

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, name, behavior.id, maybeQuestion, maybeParamType)
  }
}

case class RawBehaviorParameter(
                               id: String,
                               name: String,
                               behaviorId: String,
                               maybeQuestion: Option[String],
                               maybeParamType: Option[String]
                                 )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def behaviorId = column[String]("behavior_id")
  def maybeQuestion = column[Option[String]]("question")
  def maybeParamType = column[Option[String]]("param_type")

  def * =
    (id, name, behaviorId, maybeQuestion, maybeParamType) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
}

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithBehaviors = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  def tuple2Parameter(tuple: (RawBehaviorParameter, (RawBehavior, Team))): BehaviorParameter = {
    val raw = tuple._1
    BehaviorParameter(raw.id, raw.name, BehaviorQueries.tuple2Behavior(tuple._2), raw.maybeQuestion, raw.maybeParamType)
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithBehaviors.filter { case(param, (behavior, team)) => behavior.id === behaviorId}
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behavior: Behavior): DBIO[Seq[BehaviorParameter]] = {
    allForQuery(behavior.id).result.map(_.map(tuple2Parameter))
  }

  def nextIncompleteFor(behavior: Behavior): DBIO[Option[BehaviorParameter]] = {
    allFor(behavior).map { params =>
      params.filterNot(_.isComplete).headOption
    }
  }

  def createFor(name: String, behavior: Behavior): DBIO[BehaviorParameter] = {
    val raw = RawBehaviorParameter(IDs.next, name, behavior.id, None, None)
    (all += raw).map { _ =>
      BehaviorParameter(raw.id, raw.name, behavior, raw.maybeQuestion, raw.maybeParamType)
    }
  }

  def save(parameter: BehaviorParameter): DBIO[BehaviorParameter] = {
    val query = all.filter(_.id === parameter.id)
    val raw = parameter.toRaw
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => parameter)
  }

  def ensureFor(behavior: Behavior, params: Seq[String]): DBIO[Seq[BehaviorParameter]] = {
    for {
      _ <- all.filter(_.behaviorId === behavior.id).delete
      newParams <- DBIO.sequence(params.map(createFor(_, behavior)))
    } yield newParams
  }
}
