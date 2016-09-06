package models.bots

import models.IDs
import models.accounts.{SlackBotProfile, SlackBotProfileQueries}
import models.team.{Team, TeamQueries}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalTime}
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.bots.events.{SlackMessageContext, SlackMessageEvent}
import services.SlackService
import slack.models.Message
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class ScheduledMessage(
                            id: String,
                            text: String,
                            team: Team,
                            maybeChannelName: Option[String],
                            recurrence: Recurrence,
                            nextSentAt: DateTime,
                            createdAt: DateTime
                            ) {

  def followingSentAt: DateTime = recurrence.nextAfter(nextSentAt)

  def successResponse: String = {
    s"""OK, I will run `$text` ${recurrence.displayString.trim}.
       |
       |$nextRunsString
     """.stripMargin
  }

  def listResponse: String = {
    s"""`$text` ${recurrence.displayString.trim}
       |
       |$nextRunsString
     """.stripMargin
  }

  val nextRunDateFormatter = DateTimeFormat.forPattern("MMMM d, yyyy")
  def nextRunDateStringFor(when: DateTime): String = {
    val clarifier = if (when.toLocalDate == DateTime.now.toLocalDate) {
      " (today)"
    } else if (when.toLocalDate == DateTime.now.plusDays(1).toLocalDate) {
      " (tomorrow)"
    } else {
      ""
    }

    when.toString(nextRunDateFormatter) ++ clarifier
  }
  def nextRunTimeStringFor(when: DateTime): String = when.toString(Recurrence.timeFormatter)

  def nextRunStringFor(when: DateTime): String = s"${nextRunDateStringFor(when)} at ${nextRunTimeStringFor(when)}"

  def nextRunsString: String = {
    s"""The next two runs will be:
       | - ${nextRunStringFor(nextSentAt)}
       | - ${nextRunStringFor(followingSentAt)}
       |
     """.stripMargin
  }

  def botProfile: DBIO[Option[SlackBotProfile]] = {
    SlackBotProfileQueries.allFor(team).map(_.headOption)
  }

  // TODO: don't be slack-specific
  def send(slackService: SlackService, client: SlackRtmClient, profile: SlackBotProfile): DBIO[Unit] = {
    maybeChannelName.map { channelName =>
      val message = Message("ts", channelName, profile.userId, text, None)
      val context = SlackMessageContext(client, profile, message)
      for {
        result <- slackService.eventHandler.startInvokeConversationFor(SlackMessageEvent(context))
        _ <- withUpdatedNextTriggeredFor(DateTime.now).save
      } yield result.sendIn(context)
    }.getOrElse(DBIO.successful(Unit))
  }

  def withUpdatedNextTriggeredFor(when: DateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def save: DBIO[ScheduledMessage] = ScheduledMessageQueries.save(this)

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      id,
      text,
      team.id,
      maybeChannelName,
      recurrence.typeName,
      recurrence.frequency,
      recurrence.maybeTimeOfDay,
      recurrence.maybeMinuteOfHour,
      recurrence.maybeDayOfWeek,
      recurrence.maybeDayOfMonth,
      recurrence.maybeNthDayOfWeek,
      recurrence.maybeMonth,
      nextSentAt,
      createdAt
    )
  }
}

case class RawScheduledMessage(
                               id: String,
                               text: String,
                               teamId: String,
                               maybeChannelName: Option[String],
                               recurrenceType: String,
                               frequency: Int,
                               maybeTimeOfDay: Option[LocalTime],
                               maybeMinuteOfHour: Option[Int],
                               maybeDayOfWeek: Option[Int],
                               maybeDayOfMonth: Option[Int],
                               maybeNthDayOfWeek: Option[Int],
                               maybeMonth: Option[Int],
                               nextSentAt: DateTime,
                               createdAt: DateTime
                               )

class ScheduledMessagesTable(tag: Tag) extends Table[RawScheduledMessage](tag, "scheduled_messages") {

  def id = column[String]("id")
  def text = column[String]("text")
  def teamId = column[String]("team_id")
  def maybeChannelName = column[Option[String]]("channel_name")
  def recurrenceType = column[String]("recurrence_type")
  def frequency = column[Int]("frequency")
  def maybeTimeOfDay = column[Option[LocalTime]]("time_of_day")
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
    teamId,
    maybeChannelName,
    recurrenceType,
    frequency,
    maybeTimeOfDay,
    maybeMinuteOfHour,
    maybeDayOfWeek,
    maybeDayOfMonth,
    maybeNthDayOfWeek,
    maybeMonth,
    nextSentAt,
    createdAt
    ) <> ((RawScheduledMessage.apply _).tupled, RawScheduledMessage.unapply _)
}

object ScheduledMessageQueries {

  val all = TableQuery[ScheduledMessagesTable]
  val allWithTeam = all.join(TeamQueries.all).on(_.teamId === _.id)

  def tuple2ScheduledMessage(tuple: (RawScheduledMessage, Team)): ScheduledMessage = {
    val raw = tuple._1
    val team = tuple._2
    ScheduledMessage(
      raw.id,
      raw.text,
      team,
      raw.maybeChannelName,
      Recurrence.buildFor(raw),
      raw.nextSentAt,
      raw.createdAt
    )
  }

  def uncompiledAllToBeSentQuery(when: Rep[DateTime]) = {
    allWithTeam.filter(_._1.nextSentAt <= when)
  }
  val allToBeSentQuery = Compiled(uncompiledAllToBeSentQuery _)

  def allToBeSent: DBIO[Seq[ScheduledMessage]] = {
    allToBeSentQuery(DateTime.now).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.filter { case(msg, team) => msg.teamId === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[ScheduledMessage]] = {
    allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2ScheduledMessage)
    }
  }

  def save(trigger: ScheduledMessage): DBIO[ScheduledMessage] = {
    val raw = trigger.toRaw
    val query = all.filter(_.id === raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map { _ => trigger }
  }

  def maybeCreateFor(text: String, recurrenceText: String, team: Team, maybeChannelName: Option[String]): DBIO[Option[ScheduledMessage]] = {
    Recurrence.maybeFromText(recurrenceText).map { recurrence =>
      val newMessage = ScheduledMessage(
        IDs.next,
        text,
        team,
        maybeChannelName,
        recurrence,
        recurrence.initialAfter(DateTime.now),
        DateTime.now
      )
      newMessage.save.map(Some(_))
    }.getOrElse(DBIO.successful(None))
  }

  def uncompiledRawFindQuery(text: Rep[String], teamId: Rep[String]) = {
    all.filter(_.text === text).filter(_.teamId === teamId)
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQuery _)

  def deleteFor(text: String, team: Team): DBIO[Boolean] = {
    rawFindQueryFor(text, team.id).delete.map { r => r > 0 }
  }
}
