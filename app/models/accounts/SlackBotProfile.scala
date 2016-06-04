package models.accounts

import models._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global


case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String)

class SlackBotProfileTable(tag: Tag) extends Table[SlackBotProfile](tag, "slack_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def slackTeamId = column[String]("slack_team_id")
  def token = column[String]("token")

  def * = (userId, teamId, slackTeamId, token) <> ((SlackBotProfile.apply _).tupled, SlackBotProfile.unapply _)

}

object SlackBotProfileQueries {
  val all = TableQuery[SlackBotProfileTable]

  def uncompiledFindQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def save(profile: SlackBotProfile): DBIO[SlackBotProfile] = {
    val query = findQuery(profile.userId)
    query.result.headOption.flatMap {
      case Some(existing) => query.update(profile)
      case None => all += profile
    }.map { number =>
      profile
    }
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[SlackBotProfile]] = {
    allForTeamQuery(team.id).result
  }

  def uncompiledAllForSlackTeamQuery(slackTeamId: Rep[String]) = {
    all.filter(_.slackTeamId === slackTeamId)
  }
  val allForSlackTeamQuery = Compiled(uncompiledAllForSlackTeamQuery _)

  def allForSlackTeamId(slackTeamId: String): DBIO[Seq[SlackBotProfile]] = {
    allForSlackTeamQuery(slackTeamId).result
  }

  def ensure(userId: String, slackTeamId: String, token: String): DBIO[SlackBotProfile] = {
    val query = findQuery(userId)
    query.result.headOption.flatMap {
      case Some(existing) => {
        val profile = SlackBotProfile(userId, existing.teamId, slackTeamId, token)
        query.update(profile).map { _ => profile }
      }
      case None => Team.create.flatMap { team =>
        val newProfile = SlackBotProfile(userId, team.id, slackTeamId, token)
        (all += newProfile).map { _ => newProfile }
      }
    }
  }

}
