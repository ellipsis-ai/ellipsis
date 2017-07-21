package models.behaviors.scheduling.scheduledmessage

import java.sql.Timestamp
import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.{User, UserQueries}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.{RawRecurrence, Recurrence, RecurrenceQueries}
import models.team.{Team, TeamQueries}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawScheduledMessage(
                                id: String,
                                text: String,
                                maybeUserId: Option[String],
                                teamId: String,
                                maybeChannel: Option[String],
                                isForIndividualMembers: Boolean,
                                recurrenceId: String,
                                nextSentAt: OffsetDateTime,
                                createdAt: OffsetDateTime
                              )

class ScheduledMessagesTable(tag: Tag) extends Table[RawScheduledMessage](tag, ScheduledMessage.tableName) {

  def id = column[String]("id")
  def text = column[String]("text")
  def maybeUserId = column[Option[String]]("user_id")
  def teamId = column[String]("team_id")
  def maybeChannel = column[Option[String]]("channel_name")
  def isForIndividualMembers = column[Boolean]("is_for_individual_members")
  def recurrenceId = column[String]("recurrence_id")
  def nextSentAt = column[OffsetDateTime]("next_sent_at")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (
    id,
    text,
    maybeUserId,
    teamId,
    maybeChannel,
    isForIndividualMembers,
    recurrenceId,
    nextSentAt,
    createdAt
  ) <> ((RawScheduledMessage.apply _).tupled, RawScheduledMessage.unapply _)

}

class ScheduledMessageServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends ScheduledMessageService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[ScheduledMessagesTable]
  val allWithRecurrence = all.join(RecurrenceQueries.all).on(_.recurrenceId === _.id)
  val allWithTeam = allWithRecurrence.join(TeamQueries.all).on(_._1.teamId === _.id)
  val allWithUser = allWithTeam.joinLeft(UserQueries.all).on(_._1._1.maybeUserId === _.id)

  type TupleType = (((RawScheduledMessage, RawRecurrence), Team), Option[User])

  def tuple2ScheduledMessage(tuple: TupleType): ScheduledMessage = {
    val raw = tuple._1._1._1
    val team = tuple._1._2
    val recurrence = Recurrence.buildFor(tuple._1._1._2, team.timeZone)
    val maybeUser = tuple._2
    ScheduledMessage(
      raw.id,
      raw.text,
      maybeUser,
      team,
      raw.maybeChannel,
      raw.isForIndividualMembers,
      recurrence,
      raw.nextSentAt,
      raw.createdAt
    )
  }

  def maybeNextToBeSentAction(when: OffsetDateTime): DBIO[Option[ScheduledMessage]] = {
    for {
      maybeNextId <- Scheduled.nextToBeSentIdQueryFor(ScheduledMessage.tableName, Timestamp.from(when.toInstant)).map(_.headOption)
      maybeNext <- maybeNextId.map(findAction).getOrElse(DBIO.successful(None))
    } yield maybeNext
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithUser.filter { case(((msg, _), _), _) => msg.teamId === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): Future[Seq[ScheduledMessage]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
  }

  def uncompiledAllForChannelQuery(teamId: Rep[String], channel: Rep[String]) = {
    allWithUser.
      filter { case(((msg, _), _), _) => msg.teamId === teamId }.
      filter { case(((msg, _), _), _) => msg.maybeChannel === channel }
  }
  val allForChannelQuery = Compiled(uncompiledAllForChannelQuery _)

  def allForChannel(team: Team, channel: String): Future[Seq[ScheduledMessage]] = {
    val action = allForChannelQuery(team.id, channel).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    allWithUser.filter { case(((msg, _), _), _) => msg.id === id }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def findAction(id: String): DBIO[Option[ScheduledMessage]] = {
    findQueryFor(id).result.map { r =>
      r.headOption.map(tuple2ScheduledMessage)
    }
  }

  def find(id: String): Future[Option[ScheduledMessage]] = {
    dataService.run(findAction(id))
  }

  def uncompiledFindForTeamQuery(id: Rep[String], teamId: Rep[String]) = {
    allWithUser.filter { case (((msg, _), _), _) => msg.id === id && msg.teamId === teamId }
  }
  val findForTeamQuery = Compiled(uncompiledFindForTeamQuery _)

  def findForTeam(id: String, team: Team): Future[Option[ScheduledMessage]] = {
    val action = findForTeamQuery(id, team.id).result.map { r =>
      r.headOption.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
  }

  def saveAction(message: ScheduledMessage): DBIO[ScheduledMessage] = {
    val raw = message.toRaw
    val query = all.filter(_.id === raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map { _ => message }
  }

  def save(message: ScheduledMessage): Future[ScheduledMessage] = {
    dataService.run(saveAction(message))
  }

  def updateNextTriggeredForAction(message: ScheduledMessage): DBIO[ScheduledMessage] = {
    saveAction(message.withUpdatedNextTriggeredFor(OffsetDateTime.now))
  }

  def maybeCreateWithRecurrenceText(
                      text: String,
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannel: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledMessage]] = {
    for {
      maybeRecurrence <- dataService.recurrences.maybeCreateFromText(recurrenceText, team.timeZone)
      maybeScheduledMessage <- maybeRecurrence.map { recurrence =>
        createFor(text, recurrence, user, team, maybeChannel, isForIndividualMembers).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeScheduledMessage
  }

  def createFor(
                 text: String,
                 recurrence: Recurrence,
                 user: User,
                 team: Team,
                 maybeChannel: Option[String],
                 isForIndividualMembers: Boolean
               ): Future[ScheduledMessage] = {
    val now = Recurrence.withZone(OffsetDateTime.now, team.timeZone)
    val newMessage = ScheduledMessage(
      IDs.next,
      text,
      Some(user),
      team,
      maybeChannel,
      isForIndividualMembers,
      recurrence,
      recurrence.initialAfter(now),
      now
    )
    save(newMessage)
  }

  def uncompiledRawFindQuery(text: Rep[String], teamId: Rep[String]) = {
    all.filter(_.text === text).filter(_.teamId === teamId)
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQuery _)

  def deleteFor(text: String, team: Team): Future[Boolean] = {
    // recurrence deletes cascade to scheduled messages
    val action = for {
      allMatching <- rawFindQueryFor(text, team.id).result
      didDelete <- DBIO.sequence(allMatching.map { ea =>
        DBIO.from(dataService.recurrences.delete(ea.recurrenceId))
      }).map(didDeletes => didDeletes.contains(true))
    } yield didDelete
    dataService.run(action)
  }

  def delete(scheduledMessage: ScheduledMessage): Future[Boolean] = {
    // recurrence deletes cascade to scheduled behaviors
    dataService.recurrences.delete(scheduledMessage.recurrence.id)
  }
}
