package models.billing.active_user_record

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._


class ActiveUserRecordTable(tag: Tag) extends Table[ActiveUserRecord](tag, "active_user_records") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def userId = column[Option[String]]("user_id")
  def externalUserId = column[Option[String]]("external_user_id")
  def derivedUserId = column[String]("derived_user_id")
  def createdAt = column[OffsetDateTime]("created_at")


  def * = {

    val applyFn = (ActiveUserRecord.apply : (String, String, Option[String], Option[String], String, OffsetDateTime) => ActiveUserRecord).tupled

    (id, teamId, userId, externalUserId, derivedUserId, createdAt) <>  (applyFn, ActiveUserRecord.unapply _)
  }

}

object ActiveUserRecordQueries {

  val all = TableQuery[ActiveUserRecordTable]

  def uncompiledCountWithTeamIdAndDateQuery(teamId: Rep[String], start: Rep[OffsetDateTime], end: Rep[OffsetDateTime]) = {
    all
      .filter(r => r.teamId === teamId && r.createdAt >= start && r.createdAt <= end)
      .distinctOn(_.derivedUserId)
      .length
  }
  val compiledCountWithTeamIdAndDateQuery = Compiled(uncompiledCountWithTeamIdAndDateQuery _)
}

