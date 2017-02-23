package models.behaviors.scheduling.scheduledbehavior

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.{User, UserQueries}
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import models.behaviors.scheduling.recurrence.{RawRecurrence, Recurrence, RecurrenceQueries}
import models.team.{Team, TeamQueries}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawScheduledBehavior(
                                id: String,
                                behaviorId: String,
                                maybeUserId: Option[String],
                                teamId: String,
                                maybeChannelName: Option[String],
                                isForIndividualMembers: Boolean,
                                recurrenceId: String,
                                nextSentAt: OffsetDateTime,
                                createdAt: OffsetDateTime
                              )

class ScheduledBehaviorsTable(tag: Tag) extends Table[RawScheduledBehavior](tag, "scheduled_behaviors") {

  def id = column[String]("id")
  def behaviorId = column[String]("behavior_id")
  def maybeUserId = column[Option[String]]("user_id")
  def teamId = column[String]("team_id")
  def maybeChannelName = column[Option[String]]("channel_name")
  def isForIndividualMembers = column[Boolean]("is_for_individual_members")
  def recurrenceId = column[String]("recurrence_id")
  def nextSentAt = column[OffsetDateTime]("next_sent_at")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (
    id,
    behaviorId,
    maybeUserId,
    teamId,
    maybeChannelName,
    isForIndividualMembers,
    recurrenceId,
    nextSentAt,
    createdAt
  ) <> ((RawScheduledBehavior.apply _).tupled, RawScheduledBehavior.unapply _)

}

class ScheduledBehaviorServiceImpl @Inject() (
                                              dataServiceProvider: Provider[DataService]
                                            ) extends ScheduledBehaviorService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[ScheduledBehaviorsTable]
  val allWithBehavior = all.join(BehaviorQueries.allWithGroup).on(_.behaviorId === _._1._1.id)
  val allWithRecurrence = allWithBehavior.join(RecurrenceQueries.all).on(_._1.recurrenceId === _.id)
  val allWithTeam = allWithRecurrence.join(TeamQueries.all).on(_._1._1.teamId === _.id)
  val allWithUser = allWithTeam.joinLeft(UserQueries.all).on(_._1._1._1.maybeUserId === _.id)

  type TupleType = ((((RawScheduledBehavior, BehaviorQueries.TupleType), RawRecurrence), Team), Option[User])

  def tuple2ScheduledBehavior(tuple: TupleType): ScheduledBehavior = {
    val raw = tuple._1._1._1._1
    val behavior = BehaviorQueries.tuple2Behavior(tuple._1._1._1._2)
    val team = tuple._1._2
    val recurrence = Recurrence.buildFor(tuple._1._1._2, team.timeZone)
    val maybeUser = tuple._2
    ScheduledBehavior(
      raw.id,
      behavior,
      maybeUser,
      team,
      raw.maybeChannelName,
      raw.isForIndividualMembers,
      recurrence,
      raw.nextSentAt,
      raw.createdAt
    )
  }

  def uncompiledAllToBeSentQuery(when: Rep[OffsetDateTime]) = {
    allWithUser.filter { case((((msg, _), _), _), _) =>  msg.nextSentAt <= when }
  }
  val allToBeSentQuery = Compiled(uncompiledAllToBeSentQuery _)

  def allToBeSent: Future[Seq[ScheduledBehavior]] = {
    val action = allToBeSentQuery(OffsetDateTime.now).result.map { r =>
      r.map(tuple2ScheduledBehavior)
    }
    dataService.run(action)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithUser.filter { case((((msg, _), _), _), _) => msg.teamId === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): Future[Seq[ScheduledBehavior]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2ScheduledBehavior)
    }
    dataService.run(action)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    allWithUser.filter { case((((msg, _), _), _), _) => msg.id === id }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): Future[Option[ScheduledBehavior]] = {
    val action = findQueryFor(id).result.map { r =>
      r.headOption.map(tuple2ScheduledBehavior)
    }
    dataService.run(action)
  }

  def save(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior] = {
    val raw = scheduledBehavior.toRaw
    val query = all.filter(_.id === raw.id)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map { _ => scheduledBehavior }
    dataService.run(action)
  }

  def updateNextTriggeredFor(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior] = {
    save(scheduledBehavior.withUpdatedNextTriggeredFor(OffsetDateTime.now))
  }

  def maybeCreateFor(
                      behavior: Behavior,
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannelName: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledBehavior]] = {
    dataService.recurrences.maybeCreateFromText(recurrenceText, team.timeZone).flatMap { maybeRecurrence =>
      maybeRecurrence.map { recurrence =>
        val now = Recurrence.withZone(OffsetDateTime.now, team.timeZone)
        val newMessage = ScheduledBehavior(
          IDs.next,
          behavior,
          Some(user),
          team,
          maybeChannelName,
          isForIndividualMembers,
          recurrence,
          recurrence.initialAfter(now),
          now
        )
        save(newMessage).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  def uncompiledRawFindQuery(behaviorId: Rep[String], teamId: Rep[String]) = {
    all.filter(_.behaviorId === behaviorId).filter(_.teamId === teamId)
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQuery _)

  def deleteFor(behavior: Behavior, team: Team): Future[Boolean] = {
    val action = rawFindQueryFor(behavior.id, team.id).delete.map { r => r > 0 }
    dataService.run(action)
  }
}
