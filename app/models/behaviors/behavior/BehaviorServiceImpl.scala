package models.behaviors.behavior

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
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

  import BehaviorQueries._

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

  def allForTeam(team: Team): Future[Seq[Behavior]] = {
    val action = allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
    dataService.run(action)
  }

  def visibleForTeam(team: Team): Future[Seq[Behavior]] = {
    for {
      all <- allForTeam(team)
      dataTypes <- dataService.behaviorBackedDataTypes.allFor(team)
    } yield {
      val invisible = dataTypes.map(_.behavior)
      all.diff(invisible)
    }
  }

  def createFor(team: Team, maybeImportedId: Option[String]): Future[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, None, maybeImportedId, DateTime.now)

    val action = (all += raw).map { _ =>
      Behavior(raw.id, team, raw.maybeCurrentVersionId, raw.maybeImportedId, raw.createdAt)
    }

    dataService.run(action)
  }

  def delete(behavior: Behavior): Future[Behavior] = {
    dataService.run(findRawQueryFor(behavior.id).delete.map(_ => behavior))
  }

  def maybeCurrentVersionFor(behavior: Behavior): Future[Option[BehaviorVersion]] = {
    behavior.maybeCurrentVersionId.map { versionId =>
      dataService.behaviorVersions.findWithoutAccessCheck(versionId)
    }.getOrElse(Future.successful(None))
  }

  def unlearn(behavior: Behavior): Future[Unit] = {
    for {
      versions <- dataService.behaviorVersions.allFor(behavior)
      _ <- Future.sequence(versions.map(v => dataService.behaviorVersions.unlearn(v)))
      _ <- dataService.behaviorBackedDataTypes.maybeFor(behavior).flatMap { maybeDataType =>
        maybeDataType.map { dataType =>
          dataService.behaviorBackedDataTypes.delete(dataType).map(_ => {})
        }.getOrElse(Future.successful({}))
      }
      _ <- delete(behavior)
    } yield {}
  }

}
