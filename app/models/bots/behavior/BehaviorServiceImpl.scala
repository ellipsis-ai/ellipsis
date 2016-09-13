package models.bots.behavior

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.bots.{BehaviorVersion, BehaviorVersionQueries}
import models.team.{Team, TeamQueries}
import org.joda.time.DateTime
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


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

class BehaviorServiceImpl @Inject() (
                                      dataServiceProvider: Provider[DataService],
                                      lambdaServiceProvider: Provider[AWSLambdaService]
                                    ) extends BehaviorService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

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

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTeam.filter { case(behavior, team) => behavior.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): Future[Option[Behavior]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2Behavior))
    dataService.run(action)
  }

  def find(id: String, user: User): Future[Option[Behavior]] = {
    for {
      maybeBehavior <- findWithoutAccessCheck(id)
      canAccess <- maybeBehavior.map { behavior =>
        dataService.users.canAccess(user, behavior)
      }.getOrElse(Future.successful(false))
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

  def allForTeam(team: Team): Future[Seq[Behavior]] = {
    val action = allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
    dataService.run(action)
  }

  def createFor(team: Team, maybeImportedId: Option[String]): Future[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, None, maybeImportedId, DateTime.now)

    val action = (all += raw).map { _ =>
      Behavior(raw.id, team, raw.maybeCurrentVersionId, raw.maybeImportedId, raw.createdAt)
    }

    dataService.run(action)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def delete(behavior: Behavior): Future[Behavior] = {
    dataService.run(findQueryFor(behavior.id).delete.map(_ => behavior))
  }


  def maybeCurrentVersionFor(behavior: Behavior): Future[Option[BehaviorVersion]] = {
    behavior.maybeCurrentVersionId.map { versionId =>
      dataService.run(BehaviorVersionQueries.findWithoutAccessCheck(versionId))
    }.getOrElse(Future.successful(None))
  }

  def unlearn(behavior: Behavior): Future[Unit] = {
    val action = for {
      versions <- BehaviorVersionQueries.allFor(behavior)
      _ <- DBIO.successful(versions.map(v => v.unlearn(lambdaService)))
      _ <- DBIO.from(delete(behavior))
    } yield {}
    dataService.run(action)
  }

}
