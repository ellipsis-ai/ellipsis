package models.bots.triggers

import models.Team
import models.accounts.{SlackBotProfileQueries, SlackBotProfile}
import models.bots._
import org.joda.time.{LocalTime, DateTime}
import com.github.tototoshi.slick.PostgresJodaSupport._
import services.AWSLambdaService
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class ScheduleTrigger(
                            id: String,
                            behaviorVersion: BehaviorVersion,
                            recurrence: Recurrence,
                            nextTriggeredAt: DateTime,
                            createdAt: DateTime
                            ) extends Trigger {

  // TODO: don't be slack-specific and do something about the channel
  def run(lambdaService: AWSLambdaService, client: SlackRtmClient): DBIO[Unit] = {
    DBIO.from(behaviorVersion.unformattedResultFor(Seq(), lambdaService)).flatMap { result =>
      withUpdatedNextTriggeredFor(DateTime.now).save.map { _ =>
        val context = SlackNoMessageContext(client, "devbot")
        context.sendMessage(result)
      }
    }
  }

  def botProfile: DBIO[Option[SlackBotProfile]] = {
    SlackBotProfileQueries.allFor(behaviorVersion.team).map(_.headOption)
  }

  def withUpdatedNextTriggeredFor(when: DateTime): ScheduleTrigger = {
    this.copy(nextTriggeredAt = recurrence.nextAfter(when))
  }

  def save: DBIO[ScheduleTrigger] = ScheduleTriggerQueries.save(this)

  def toRaw: RawScheduleTrigger = {
    RawScheduleTrigger(
      id,
      behaviorVersion.id,
      recurrence.typeName,
      recurrence.frequency,
      recurrence.maybeTimeOfDay,
      recurrence.maybeMinuteOfHour,
      recurrence.maybeDayOfWeek,
      recurrence.maybeDayOfMonth,
      recurrence.maybeNthDayOfWeek,
      recurrence.maybeMonth,
      nextTriggeredAt,
      createdAt
    )
  }
}

case class RawScheduleTrigger(
                             id: String,
                             behaviorVersionId: String,
                             recurrenceType: String,
                             frequency: Int,
                             maybeTimeOfDay: Option[LocalTime],
                             maybeMinuteOfHour: Option[Int],
                             maybeDayOfWeek: Option[Int],
                             maybeDayOfMonth: Option[Int],
                             maybeNthDayOfWeek: Option[Int],
                             maybeMonth: Option[Int],
                             nextTriggeredAt: DateTime,
                             createdAt: DateTime
                               )

class ScheduleTriggersTable(tag: Tag) extends Table[RawScheduleTrigger](tag, "schedule_triggers") {

  def id = column[String]("id")
  def behaviorVersionId = column[String]("behavior_version_id")
  def recurrenceType = column[String]("recurrence_type")
  def frequency = column[Int]("frequency")
  def maybeTimeOfDay = column[Option[LocalTime]]("time_of_day")
  def maybeMinuteOfHour = column[Option[Int]]("minute_of_hour")
  def maybeDayOfWeek = column[Option[Int]]("day_of_week")
  def maybeDayOfMonth = column[Option[Int]]("day_of_month")
  def maybeNthDayOfWeek = column[Option[Int]]("nth_day_of_week")
  def maybeMonth = column[Option[Int]]("month")
  def nextTriggeredAt = column[DateTime]("next_triggered_at")
  def createdAt = column[DateTime]("created_at")

  def * = (
    id,
    behaviorVersionId,
    recurrenceType,
    frequency,
    maybeTimeOfDay,
    maybeMinuteOfHour,
    maybeDayOfWeek,
    maybeDayOfMonth,
    maybeNthDayOfWeek,
    maybeMonth,
    nextTriggeredAt,
    createdAt
    ) <> ((RawScheduleTrigger.apply _).tupled, RawScheduleTrigger.unapply _)
}

object ScheduleTriggerQueries {

  val all = TableQuery[ScheduleTriggersTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1.id)

  def tuple2Trigger(tuple: (RawScheduleTrigger, (RawBehaviorVersion, (RawBehavior, Team)))): ScheduleTrigger = {
    val raw = tuple._1
    ScheduleTrigger(
      raw.id,
      BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2),
      Recurrence.buildFor(raw),
      raw.nextTriggeredAt,
      raw.createdAt
    )
  }

  def uncompiledAllToBeTriggeredQuery(when: Rep[DateTime]) = {
    allWithBehaviorVersion.filter { case(trigger, _) => trigger.nextTriggeredAt <= when }
  }
  val allToBeTriggeredQuery = Compiled(uncompiledAllToBeTriggeredQuery _)

  def allToBeTriggered: DBIO[Seq[ScheduleTrigger]] = {
    allToBeTriggeredQuery(DateTime.now).result.map { r =>
      r.map(tuple2Trigger)
    }
  }

  def save(trigger: ScheduleTrigger): DBIO[ScheduleTrigger] = {
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
}
