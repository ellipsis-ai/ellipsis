package models.bots

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.accounts.User
import models.{IDs, Team}
import org.joda.time.DateTime
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class Behavior(
                    id: String,
                    team: Team,
                    maybeCurrentVersionId: Option[String],
                    createdAt: DateTime
                    ) {

  def maybeCurrentVersion: DBIO[Option[BehaviorVersion]] = {
    maybeCurrentVersionId.map { versionId =>
      BehaviorVersionQueries.findWithoutAccessCheck(versionId)
    }.getOrElse(DBIO.successful(None))
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    lambdaService.deleteFunction(id)
    BehaviorQueries.delete(this).map(_ => Unit)
  }

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeCurrentVersionId, createdAt)
  }

}

case class RawBehavior(
                       id: String,
                       teamId: String,
                       maybeCurrentVersionId: Option[String],
                       createdAt: DateTime
                       )

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeCurrentVersionId = column[Option[String]]("current_version_id")
  def createdAt = column[DateTime]("created_at")

  def * = (id, teamId, maybeCurrentVersionId, createdAt) <> ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val raw = tuple._1
    Behavior(
      raw.id,
      tuple._2,
      raw.maybeCurrentVersionId,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTeam.filter { case(behavior, team) => behavior.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  // doesn't check if accessible to a user so private
  private def find(id: String): DBIO[Option[Behavior]] = {
    findQuery(id).result.map(_.headOption.map(tuple2Behavior))
  }

  def find(id: String, user: User): DBIO[Option[Behavior]] = {
    for {
      maybeBehavior <- find(id)
      maybeAccessibleBehavior <- maybeBehavior.map { behavior =>
        user.canAccess(behavior.team).map { canAccess =>
          if (canAccess) {
            Some(behavior)
          } else {
            None
          }
        }
      }.getOrElse(DBIO.successful(None))
    } yield maybeAccessibleBehavior
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[Behavior]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
  }

  def createFor(team: Team): DBIO[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, None, DateTime.now)

    (all += raw).map { _ =>
      Behavior(raw.id, team, raw.maybeCurrentVersionId, raw.createdAt)
    }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def delete(behavior: Behavior): DBIO[Behavior] = {
    findQueryFor(behavior.id).delete.map(_ => behavior)
  }

}
