package models.behaviors.scheduling.scheduledmessage

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.{User, UserQueries}
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
                              maybeChannelName: Option[String],
                              isForIndividualMembers: Boolean,
                              recurrenceId: String,
                              nextSentAt: OffsetDateTime,
                              createdAt: OffsetDateTime
                              )

class ScheduledMessagesTable(tag: Tag) extends Table[RawScheduledMessage](tag, "scheduled_messages") {

  def id = column[String]("id")
  def text = column[String]("text")
  def maybeUserId = column[Option[String]]("user_id")
  def teamId = column[String]("team_id")
  def maybeChannelName = column[Option[String]]("channel_name")
  def isForIndividualMembers = column[Boolean]("is_for_individual_members")
  def recurrenceId = column[String]("recurrence_id")
  def nextSentAt = column[OffsetDateTime]("next_sent_at")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (
    id,
    text,
    maybeUserId,
    teamId,
    maybeChannelName,
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
      raw.maybeChannelName,
      raw.isForIndividualMembers,
      recurrence,
      raw.nextSentAt,
      raw.createdAt
    )
  }

  def uncompiledAllToBeSentQuery(when: Rep[OffsetDateTime]) = {
    allWithUser.filter { case(((msg, _), _), _) =>  msg.nextSentAt <= when }
  }
  val allToBeSentQuery = Compiled(uncompiledAllToBeSentQuery _)

  def allToBeSent: Future[Seq[ScheduledMessage]] = {
    val action = allToBeSentQuery(OffsetDateTime.now).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
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

  def uncompiledFindQueryFor(id: Rep[String]) = {
    allWithUser.filter { case(((msg, _), _), _) => msg.id === id }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): Future[Option[ScheduledMessage]] = {
    val action = findQueryFor(id).result.map { r =>
      r.headOption.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
  }

  def save(message: ScheduledMessage): Future[ScheduledMessage] = {
    val raw = message.toRaw
    val query = all.filter(_.id === raw.id)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map { _ => message }
    dataService.run(action)
  }

  def updateNextTriggeredFor(message: ScheduledMessage): Future[ScheduledMessage] = {
    save(message.withUpdatedNextTriggeredFor(OffsetDateTime.now))
  }

  def maybeCreateFor(
                      text: String,
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannelName: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledMessage]] = {
    dataService.recurrences.maybeCreateFromText(recurrenceText, team.timeZone).flatMap { maybeRecurrence =>
      maybeRecurrence.map { recurrence =>
        val now = Recurrence.withZone(OffsetDateTime.now, team.timeZone)
        val newMessage = ScheduledMessage(
          IDs.next,
          text,
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
}
