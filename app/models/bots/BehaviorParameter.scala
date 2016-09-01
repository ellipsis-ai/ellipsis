package models.bots

import models.accounts.user.User
import models.{IDs, Team}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorParameter(
                            id: String,
                            name: String,
                            rank: Int,
                            behaviorVersion: BehaviorVersion,
                            maybeQuestion: Option[String],
                            maybeParamType: Option[String]
                              ) {

  def question: String = maybeQuestion.getOrElse("")

  def isComplete: Boolean = {
    maybeQuestion.isDefined
  }

  def save: DBIO[BehaviorParameter] = BehaviorParameterQueries.save(this)

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, name, rank, behaviorVersion.id, maybeQuestion, maybeParamType)
  }
}

case class RawBehaviorParameter(
                               id: String,
                               name: String,
                               rank: Int,
                               behaviorVersionId: String,
                               maybeQuestion: Option[String],
                               maybeParamType: Option[String]
                                 )

class BehaviorParametersTable(tag: Tag) extends Table[RawBehaviorParameter](tag, "behavior_parameters") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def rank = column[Int]("rank")
  def behaviorVersionId = column[String]("behavior_version_id")
  def maybeQuestion = column[Option[String]]("question")
  def maybeParamType = column[Option[String]]("param_type")

  def * =
    (id, name, rank, behaviorVersionId, maybeQuestion, maybeParamType) <> ((RawBehaviorParameter.apply _).tupled, RawBehaviorParameter.unapply _)
}

object BehaviorParameterQueries {

  val all = TableQuery[BehaviorParametersTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  type TupleType = (RawBehaviorParameter, ((RawBehaviorVersion, Option[User]), (RawBehavior, Team)))

  def tuple2Parameter(tuple: TupleType): BehaviorParameter = {
    val raw = tuple._1
    BehaviorParameter(raw.id, raw.name, raw.rank, BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2), raw.maybeQuestion, raw.maybeParamType)
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(param, ((behaviorVersion, user), team)) => behaviorVersion.id === behaviorVersionId}.
      sortBy { case(param, _) => param.rank.asc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behaviorVersion: BehaviorVersion): DBIO[Seq[BehaviorParameter]] = {
    allForQuery(behaviorVersion.id).result.map(_.map(tuple2Parameter))
  }

  def nextIncompleteFor(behaviorVersion: BehaviorVersion): DBIO[Option[BehaviorParameter]] = {
    allFor(behaviorVersion).map { params =>
      params.filterNot(_.isComplete).headOption
    }
  }

  def createFor(name: String, maybeQuestion: Option[String], rank: Int, behaviorVersion: BehaviorVersion): DBIO[BehaviorParameter] = {
    val raw = RawBehaviorParameter(IDs.next, name, rank, behaviorVersion.id, maybeQuestion, None)
    (all += raw).map { _ =>
      BehaviorParameter(raw.id, raw.name, raw.rank, behaviorVersion, raw.maybeQuestion, raw.maybeParamType)
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

  def ensureFor(behaviorVersion: BehaviorVersion, params: Seq[(String, Option[String])]): DBIO[Seq[BehaviorParameter]] = {
    for {
      _ <- all.filter(_.behaviorVersionId === behaviorVersion.id).delete
      newParams <- DBIO.sequence(params.zipWithIndex.map { case((name, maybeQuestion), i) =>
        createFor(name, maybeQuestion, i + 1, behaviorVersion)
      })
    } yield newParams
  }
}
