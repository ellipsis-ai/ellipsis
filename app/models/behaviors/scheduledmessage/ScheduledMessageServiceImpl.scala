package models.behaviors.scheduledmessage

import java.time.DayOfWeek
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

case class RawScheduledMessage(base: RawScheduledMessageBase, options: RawScheduledMessageOptions) {

  val id: String = base.id
  val text: String = base.text
  val maybeUserId: Option[String] = base.maybeUserId
  val teamId: String = base.teamId
  val maybeChannelName: Option[String] = base.maybeChannelName
  val isForIndividualMembers: Boolean = base.isForIndividualMembers
  val recurrenceType: String = base.recurrenceType
  val frequency: Int = base.frequency
  val nextSentAt: DateTime = base.nextSentAt
  val createdAt: DateTime = base.createdAt

  val maybeTimeOfDay = options.maybeTimeOfDay
  val maybeTimeZone = options.maybeTimeZone
  val maybeMinuteOfHour = options.maybeMinuteOfHour
  val maybeDayOfWeek = options.maybeDayOfWeek.map(DayOfWeek.of)
  val maybeMonday = options.maybeMonday
  val maybeTuesday = options.maybeTuesday
  val maybeWednesday = options.maybeWednesday
  val maybeThursday = options.maybeThursday
  val maybeFriday = options.maybeFriday
  val maybeSaturday = options.maybeSaturday
  val maybeSunday = options.maybeSunday
  val maybeDayOfMonth = options.maybeDayOfMonth
  val maybeNthDayOfWeek = options.maybeNthDayOfWeek
  val maybeMonth = options.maybeMonth

  val daysOfWeek: Seq[DayOfWeek] = {
    DayOfWeek.values.zip(
      Seq(
        maybeMonday,
        maybeTuesday,
        maybeWednesday,
        maybeThursday,
        maybeFriday,
        maybeSaturday,
        maybeSunday
      )
    ).
      filter { case(day, maybeOn) => maybeOn.exists(identity) }.
      map { case(day, _) => day }
  }

}

case class RawScheduledMessageBase(
                                    id: String,
                                    text: String,
                                    maybeUserId: Option[String],
                                    teamId: String,
                                    maybeChannelName: Option[String],
                                    isForIndividualMembers: Boolean,
                                    recurrenceType: String,
                                    frequency: Int,
                                    nextSentAt: DateTime,
                                    createdAt: DateTime
                                  )

case class RawScheduledMessageOptions(
                                       maybeTimeOfDay: Option[LocalTime],
                                       maybeTimeZone: Option[DateTimeZone],
                                       maybeMinuteOfHour: Option[Int],
                                       maybeDayOfWeek: Option[Int],
                                       maybeMonday: Option[Boolean],
                                       maybeTuesday: Option[Boolean],
                                       maybeWednesday: Option[Boolean],
                                       maybeThursday: Option[Boolean],
                                       maybeFriday: Option[Boolean],
                                       maybeSaturday: Option[Boolean],
                                       maybeSunday: Option[Boolean],
                                       maybeDayOfMonth: Option[Int],
                                       maybeNthDayOfWeek: Option[Int],
                                       maybeMonth: Option[Int]
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
  def maybeMonday = column[Option[Boolean]]("monday")
  def maybeTuesday = column[Option[Boolean]]("tuesday")
  def maybeWednesday = column[Option[Boolean]]("wednesday")
  def maybeThursday = column[Option[Boolean]]("thursday")
  def maybeFriday = column[Option[Boolean]]("friday")
  def maybeSaturday = column[Option[Boolean]]("saturday")
  def maybeSunday = column[Option[Boolean]]("sunday")
  def maybeDayOfMonth = column[Option[Int]]("day_of_month")
  def maybeNthDayOfWeek = column[Option[Int]]("nth_day_of_week")
  def maybeMonth = column[Option[Int]]("month")
  def nextSentAt = column[DateTime]("next_sent_at")
  def createdAt = column[DateTime]("created_at")

  private type ScheduledMessageBaseTupleType = (
    String,
    String,
    Option[String],
    String,
    Option[String],
    Boolean,
    String,
    Int,
    DateTime,
    DateTime
  )

  private type ScheduledMessageOptionsTupleType = (
    Option[LocalTime],
    Option[DateTimeZone],
    Option[Int],
    Option[Int],
    Option[Boolean],
    Option[Boolean],
    Option[Boolean],
    Option[Boolean],
    Option[Boolean],
    Option[Boolean],
    Option[Boolean],
    Option[Int],
    Option[Int],
    Option[Int]
  )

  private type ScheduledMessageTupleType = (ScheduledMessageBaseTupleType, ScheduledMessageOptionsTupleType)

  private val scheduledMessageShapedValue = (
    (
      id,
      text,
      maybeUserId,
      teamId,
      maybeChannelName,
      isForIndividualMembers,
      recurrenceType,
      frequency,
      nextSentAt,
      createdAt
    ), (
      maybeTimeOfDay,
      maybeTimeZone,
      maybeMinuteOfHour,
      maybeDayOfWeek,
      maybeMonday,
      maybeTuesday,
      maybeWednesday,
      maybeThursday,
      maybeFriday,
      maybeSaturday,
      maybeSunday,
      maybeDayOfMonth,
      maybeNthDayOfWeek,
      maybeMonth
    )
  ).shaped[ScheduledMessageTupleType]

  private val toRawScheduledMessage: (ScheduledMessageTupleType => RawScheduledMessage) = { tuple =>
    val base = RawScheduledMessageBase.tupled.apply(tuple._1)
    val options = RawScheduledMessageOptions.tupled.apply(tuple._2)
    RawScheduledMessage(base, options)
  }

  private val toTuple: (RawScheduledMessage => Option[ScheduledMessageTupleType]) = { raw =>
    Some(RawScheduledMessageBase.unapply(raw.base).get, RawScheduledMessageOptions.unapply(raw.options).get)
  }

  def * = scheduledMessageShapedValue <> (toRawScheduledMessage, toTuple)

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
    save(message.withUpdatedNextTriggeredFor(DateTime.now))
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
