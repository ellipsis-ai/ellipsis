package models.behaviors.scheduledmessage

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.accounts.user.{User, UserQueries}
import models.team.{Team, TeamQueries}
import org.joda.time.{DateTime, DateTimeZone, LocalTime}
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawScheduledMessage(
                                id: String,
                                text: String,
                                maybeUserId: Option[String],
                                teamId: String,
                                maybeChannelName: Option[String],
                                isForIndividualMembers: Boolean,
                                recurrenceType: String,
                                frequency: Int,
                                maybeTimeOfDay: Option[LocalTime],
                                maybeTimeZone: Option[DateTimeZone],
                                maybeMinuteOfHour: Option[Int],
                                maybeDayOfWeek: Option[Int],
                                maybeDayOfMonth: Option[Int],
                                maybeNthDayOfWeek: Option[Int],
                                maybeMonth: Option[Int],
                                nextSentAt: DateTime,
                                createdAt: DateTime
                              )

class ScheduledMessagesTable(tag: Tag) extends Table[RawScheduledMessage](tag, "scheduled_messages") {

  import models.MappedColumnTypeImplicits._

  def id = column[String]("id")
  def text = column[String]("text")
  def maybeUserId = column[Option[String]]("user_id")
  def teamId = column[String]("team_id")
  def maybeChannelName = column[Option[String]]("channel_name")
  def isForIndividualMembers = column[Boolean]("is_for_individual_members")
  def recurrenceType = column[String]("recurrence_type")
  def frequency = column[Int]("frequency")
  def maybeTimeOfDay = column[Option[LocalTime]]("time_of_day")
  def maybeTimeZone = column[Option[DateTimeZone]]("time_zone")
  def maybeMinuteOfHour = column[Option[Int]]("minute_of_hour")
  def maybeDayOfWeek = column[Option[Int]]("day_of_week")
  def maybeDayOfMonth = column[Option[Int]]("day_of_month")
  def maybeNthDayOfWeek = column[Option[Int]]("nth_day_of_week")
  def maybeMonth = column[Option[Int]]("month")
  def nextSentAt = column[DateTime]("next_sent_at")
  def createdAt = column[DateTime]("created_at")

  def * = (
    id,
    text,
    maybeUserId,
    teamId,
    maybeChannelName,
    isForIndividualMembers,
    recurrenceType,
    frequency,
    maybeTimeOfDay,
    maybeTimeZone,
    maybeMinuteOfHour,
    maybeDayOfWeek,
    maybeDayOfMonth,
    maybeNthDayOfWeek,
    maybeMonth,
    nextSentAt,
    createdAt
    ) <> ((RawScheduledMessage.apply _).tupled, RawScheduledMessage.unapply _)
}

class ScheduledMessageServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends ScheduledMessageService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[ScheduledMessagesTable]
  val allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)
  val allWithUser = allWithTeam.joinLeft(UserQueries.all).on(_._1.maybeUserId === _.id)

  type TupleType = ((RawScheduledMessage, Team), Option[User])

  def tuple2ScheduledMessage(tuple: TupleType): ScheduledMessage = {
    val raw = tuple._1._1
    val team = tuple._1._2
    val maybeUser = tuple._2
    ScheduledMessage(
      raw.id,
      raw.text,
      maybeUser,
      team,
      raw.maybeChannelName,
      raw.isForIndividualMembers,
      Recurrence.buildFor(raw, team.timeZone),
      raw.nextSentAt,
      raw.createdAt
    )
  }

  def uncompiledAllToBeSentQuery(when: Rep[DateTime]) = {
    allWithUser.filter { case((msg, team), user) =>  msg.nextSentAt <= when }
  }
  val allToBeSentQuery = Compiled(uncompiledAllToBeSentQuery _)

  def allToBeSent: Future[Seq[ScheduledMessage]] = {
    val action = allToBeSentQuery(DateTime.now).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithUser.filter { case((msg, team), user) => msg.teamId === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): Future[Seq[ScheduledMessage]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
    dataService.run(action)
  }

  def save(trigger: ScheduledMessage): Future[ScheduledMessage] = {
    val raw = trigger.toRaw
    val query = all.filter(_.id === raw.id)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map { _ => trigger }
    dataService.run(action)
  }

  def maybeCreateFor(
                      text: String,
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannelName: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledMessage]] = {
    Recurrence.maybeFromText(recurrenceText, team.timeZone).map { recurrence =>
      val now = DateTime.now.withZone(team.timeZone)
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

  def uncompiledRawFindQuery(text: Rep[String], teamId: Rep[String]) = {
    all.filter(_.text === text).filter(_.teamId === teamId)
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQuery _)

  def deleteFor(text: String, team: Team): Future[Boolean] = {
    val action = rawFindQueryFor(text, team.id).delete.map { r => r > 0 }
    dataService.run(action)
  }
}
