package models.behaviors.behaviorversionuserinvolvement

import java.time.OffsetDateTime

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class RawBehaviorVersionUserInvolvement(
                                               id: String,
                                               behaviorVersionId: String,
                                               userId: String,
                                               createdAt: OffsetDateTime
                                              )

class BehaviorVersionUserInvolvementsTable(tag: Tag) extends Table[RawBehaviorVersionUserInvolvement](tag, "behavior_version_user_involvements") {

  def id = column[String]("id")
  def behaviorVersionId = column[String]("behavior_version_id")
  def userId = column[String]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, behaviorVersionId, userId, createdAt) <>
    ((RawBehaviorVersionUserInvolvement.apply _).tupled, RawBehaviorVersionUserInvolvement.unapply _)
}

class BehaviorVersionUserInvolvementServiceImpl @Inject()(
                                                          dataServiceProvider: Provider[DataService],
                                                          implicit val ec: ExecutionContext
                                                        ) extends BehaviorVersionUserInvolvementService {

  def dataService = dataServiceProvider.get

  import BehaviorVersionUserInvolvementQueries._

  def createAllFor(
                    behaviorVersion: BehaviorVersion,
                    users: Seq[User],
                    createdAt: OffsetDateTime
                  ): Future[Seq[BehaviorVersionUserInvolvement]] = {
    val instances = users.map { ea =>
      BehaviorVersionUserInvolvement(
        IDs.next,
        behaviorVersion,
        ea,
        createdAt
      )
    }

    val action = (all ++= instances.map(_.toRaw)).map { _ => instances}
    dataService.run(action)
  }

  def findAllForTeamBetween(team: Team, start: OffsetDateTime, end: OffsetDateTime): Future[Seq[BehaviorVersionUserInvolvement]] = {
    val action = findAllForTeamBetweenQuery(team.id, start, end).result.map { r =>
      r.map(tuple2Involvement)
    }
    dataService.run(action)
  }

}
