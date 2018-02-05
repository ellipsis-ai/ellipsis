package models.team_stats

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import play.api.libs.json.JsValue
import play.libs.Json

class TeamStatsTable(tag: Tag) extends Table[TeamStat](tag, "team_stats") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def teamId = column[String]("team_id")
  def start_time = column[OffsetDateTime]("start_time")
  def end_time = column[OffsetDateTime]("end_time")
  def value = column[BigDecimal]("value")
  def about = column[JsValue]("about")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = {
    // need to tell the compiler which apply method to use
    val applyFn = (TeamStat.apply : (String, String, String, OffsetDateTime, OffsetDateTime, BigDecimal, JsValue, OffsetDateTime) => TeamStat).tupled

    (id, name, teamId, start_time, end_time, value, about, createdAt) <>  (applyFn, TeamStat.unapply _)
  }

}

object TeamStatQueries {

  val all = TableQuery[TeamStatsTable]

  def allDailyActiveUsers(start: OffsetDateTime, end: OffsetDateTime, teamId: String) = {
    all
  }

}
