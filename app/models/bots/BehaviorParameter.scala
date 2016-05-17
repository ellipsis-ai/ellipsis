package models.bots

import models.{IDs, Team}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorParameter(
                            id: String,
                            name: String,
                            rank: Int,
                            behavior: Behavior,
                            maybeQuestion: Option[String],
                            maybeParamType: Option[String]
                              ) {

  def question: String = maybeQuestion.getOrElse("")

  def isComplete: Boolean = {
    maybeQuestion.isDefined
  }

  def save: DBIO[BehaviorParameter] = BehaviorParameterQueries.save(this)

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, name, rank, behavior.id, maybeQuestion, maybeParamType)
  }
}

case class RawBehaviorParameter(
                               id: String,
                               name: String,
                               rank: Int,
                               behaviorId: String,
                               maybeQuestion: Option[String],
                               maybeParamType: Option[String]
                                 )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def rank = column[Int]("rank")
  def behaviorId = column[String]("behavior_id")
  def maybeQuestion = column[Option[String]]("question")
  def maybeParamType = column[Option[String]]("param_type")

  def * =
    (id, name, rank, behaviorId, maybeQuestion, maybeParamType) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
}

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithBehaviors = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  type TupleType = (RawBehaviorParameter, (RawBehavior, Team))

  def tuple2Parameter(tuple: TupleType): BehaviorParameter = {
    val raw = tuple._1
    BehaviorParameter(raw.id, raw.name, raw.rank, BehaviorQueries.tuple2Behavior(tuple._2), raw.maybeQuestion, raw.maybeParamType)
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithBehaviors.
      filter { case(param, (behavior, team)) => behavior.id === behaviorId}.
      sortBy { case(param, _) => param.rank.asc }
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

  def createFor(name: String, maybeQuestion: Option[String], rank: Int, behavior: Behavior): DBIO[BehaviorParameter] = {
    val raw = RawBehaviorParameter(IDs.next, name, rank, behavior.id, maybeQuestion, None)
    (all += raw).map { _ =>
      BehaviorParameter(raw.id, raw.name, raw.rank, behavior, raw.maybeQuestion, raw.maybeParamType)
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

  def ensureFor(behavior: Behavior, params: Seq[(String, Option[String])]): DBIO[Seq[BehaviorParameter]] = {
    for {
      _ <- all.filter(_.behaviorId === behavior.id).delete
      newParams <- DBIO.sequence(params.zipWithIndex.map { case((name, maybeQuestion), i) =>
        createFor(name, maybeQuestion, i + 1, behavior)
      })
    } yield newParams
  }
}
