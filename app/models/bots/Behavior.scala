package models.bots

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.accounts.user.User
import models.IDs
import models.team.{Team, TeamQueries}
import org.joda.time.DateTime
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class Behavior(
                    id: String,
                    team: Team,
                    maybeCurrentVersionId: Option[String],
                    maybeImportedId: Option[String],
                    createdAt: DateTime
                    ) {

  def maybeCurrentVersion: DBIO[Option[BehaviorVersion]] = {
    maybeCurrentVersionId.map { versionId =>
      BehaviorVersionQueries.findWithoutAccessCheck(versionId)
    }.getOrElse(DBIO.successful(None))
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    for {
      versions <- BehaviorVersionQueries.allFor(this)
      _ <- DBIO.successful(versions.map(v => v.unlearn(lambdaService)))
      _ <- BehaviorQueries.delete(this)
    } yield Unit
  }

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeCurrentVersionId, maybeImportedId, createdAt)
  }

}

case class RawBehavior(
                       id: String,
                       teamId: String,
                       maybeCurrentVersionId: Option[String],
                       maybeImportedId: Option[String],
                       createdAt: DateTime
                       )

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeCurrentVersionId = column[Option[String]]("current_version_id")
  def maybeImportedId = column[Option[String]]("imported_id")
  def createdAt = column[DateTime]("created_at")

  def * = (id, teamId, maybeCurrentVersionId, maybeImportedId, createdAt) <> ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val raw = tuple._1
    Behavior(
      raw.id,
      tuple._2,
      raw.maybeCurrentVersionId,
      raw.maybeImportedId,
      raw.createdAt
    )
  }

  def allCurrentVersionIds: DBIO[Seq[String]] = {
    all.result.map { r =>
      r.flatMap(_.maybeCurrentVersionId)
    }
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTeam.filter { case(behavior, team) => behavior.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): DBIO[Option[Behavior]] = {
    findQuery(id).result.map(_.headOption.map(tuple2Behavior))
  }

  def find(id: String, user: User, dataService: DataService): DBIO[Option[Behavior]] = {
    for {
      maybeBehavior <- findWithoutAccessCheck(id)
      canAccess <- maybeBehavior.map { behavior =>
        DBIO.from(dataService.users.canAccess(user, behavior))
      }.getOrElse(DBIO.successful(false))
    } yield {
      if (canAccess) {
        maybeBehavior
      } else {
        None
      }
    }
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[Behavior]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
  }

  def createFor(team: Team, maybeImportedId: Option[String]): DBIO[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, None, maybeImportedId, DateTime.now)

    (all += raw).map { _ =>
      Behavior(raw.id, team, raw.maybeCurrentVersionId, raw.maybeImportedId, raw.createdAt)
    }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def delete(behavior: Behavior): DBIO[Behavior] = {
    findQueryFor(behavior.id).delete.map(_ => behavior)
  }

}
