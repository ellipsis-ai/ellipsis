package models.accounts

import models.team.Team
import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global


case class SlackBotProfile(userId: String, teamId: String, slackTeamId: String, token: String, createdAt: DateTime)

class SlackBotProfileTable(tag: Tag) extends Table[SlackBotProfile](tag, "slack_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def slackTeamId = column[String]("slack_team_id")
  def token = column[String]("token")
  def createdAt = column[DateTime]("created_at")

  def * = (userId, teamId, slackTeamId, token, createdAt) <> ((SlackBotProfile.apply _).tupled, SlackBotProfile.unapply _)

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

  def uncompiledAllSinceQuery(when: Rep[DateTime]) = {
    all.filter(_.createdAt >= when)
  }
  val allSinceQuery = Compiled(uncompiledAllSinceQuery _)

  def allSince(when: DateTime): DBIO[Seq[SlackBotProfile]] = {
    allSinceQuery(when).result
  }

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String, dataService: DataService): DBIO[SlackBotProfile] = {
    val query = findQuery(userId)
    query.result.headOption.flatMap {
      case Some(existing) => {
        val profile = SlackBotProfile(userId, existing.teamId, slackTeamId, token, existing.createdAt)
        for {
          maybeTeam <- DBIO.from(dataService.teams.find(existing.teamId))
          _ <- query.update(profile)
          _ <- maybeTeam.map { team =>
            DBIO.from(dataService.teams.setInitialNameFor(team, slackTeamName))
          }.getOrElse(DBIO.successful(Unit))
        } yield profile
      }
      case None => DBIO.from(dataService.teams.create(slackTeamName)).flatMap { team =>
        val newProfile = SlackBotProfile(userId, team.id, slackTeamId, token, DateTime.now)
        (all += newProfile).map { _ => newProfile }
      }
    }
  }

}
