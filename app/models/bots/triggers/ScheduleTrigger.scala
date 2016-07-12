package models.bots.triggers

import models.Team
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
                            nextTriggered: DateTime,
                            createdAt: DateTime
                            ) extends Trigger {

  // TODO: don't be slack-specific and do something about the channel
  def run(lambdaService: AWSLambdaService, client: SlackRtmClient): DBIO[Unit] = {
    DBIO.from(behaviorVersion.unformattedResultFor(Seq(), lambdaService)).map { result =>
      val context = SlackNoMessageContext(client, "general")
      context.sendMessage(result)
    }
  }

  def withUpdatedNextTriggeredFor(when: DateTime): ScheduleTrigger = {
    this.copy(nextTriggered = recurrence.nextAfter(when))
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
                             nextTriggered: DateTime,
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
  def nextTriggered = column[DateTime]("next_triggered")
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
    nextTriggered,
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
      raw.nextTriggered,
      raw.createdAt
    )
  }

  def uncompiledAllToBeTriggeredQuery(when: Rep[DateTime]) = {
    allWithBehaviorVersion.filter { case(trigger, _) => trigger.nextTriggered <= when }
  }
  val allToBeTriggeredQuery = Compiled(uncompiledAllToBeTriggeredQuery _)

  def allToBeTriggered: DBIO[Seq[ScheduleTrigger]] = {
    allToBeTriggeredQuery(DateTime.now).result.map { r =>
      r.map(tuple2Trigger)
    }
  }
}
