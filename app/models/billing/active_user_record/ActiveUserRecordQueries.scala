package models.billing.active_user_record

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._
import models.accounts.user.UserQueries


class ActiveUserRecordTable(tag: Tag) extends Table[ActiveUserRecord](tag, "active_user_records") {

  def id = column[String]("id", O.PrimaryKey)
  def userId = column[String]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")


  def * = {
    val applyFn = (ActiveUserRecord.apply : (String, String, OffsetDateTime) => ActiveUserRecord).tupled
    (id, userId, createdAt) <>  (applyFn, ActiveUserRecord.unapply _)
  }

}

object ActiveUserRecordQueries {

  val all = TableQuery[ActiveUserRecordTable]
  val allWithUser = all.join(UserQueries.all).on(_.userId === _.id)

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledAllForTeam(teamId: Rep[String]) = {
    allWithUser.filter { case(_, user) => user.teamId === teamId }
  }

  def uncompiledAllForTeamBetweenDates(teamId: Rep[String], start: Rep[OffsetDateTime], end: Rep[OffsetDateTime]) = {
    uncompiledAllForTeam(teamId).filter { case(aur, user) => (aur.createdAt >= start && aur.createdAt <= end) }
  }

  def uncompiledCountForTeamBetweenDates(teamId: Rep[String], start: Rep[OffsetDateTime], end: Rep[OffsetDateTime]) = {
    uncompiledAllForTeamBetweenDates(teamId, start, end)
      .groupBy(_._1.userId)
      .map { case(userId, recordsQuery) => (userId, recordsQuery.length) }
  }
  val compiledCountForTeamBetweenDates = uncompiledCountForTeamBetweenDates _
}

