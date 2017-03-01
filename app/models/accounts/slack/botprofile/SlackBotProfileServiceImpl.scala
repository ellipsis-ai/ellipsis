package models.accounts.slack.botprofile

import java.time.OffsetDateTime
import javax.inject.{Inject, Provider}

import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SlackBotProfileTable(tag: Tag) extends Table[SlackBotProfile](tag, "slack_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def slackTeamId = column[String]("slack_team_id")
  def token = column[String]("token")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (userId, teamId, slackTeamId, token, createdAt) <> ((SlackBotProfile.apply _).tupled, SlackBotProfile.unapply _)

}

class SlackBotProfileServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService]
                                        ) extends SlackBotProfileService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[SlackBotProfileTable]

  def allProfiles: Future[Seq[SlackBotProfile]] = dataService.run(all.result)

  def uncompiledFindQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): Future[Seq[SlackBotProfile]] = {
    dataService.run(allForTeamQuery(team.id).result)
  }

  def uncompiledAllForSlackTeamQuery(slackTeamId: Rep[String]) = {
    all.filter(_.slackTeamId === slackTeamId)
  }
  val allForSlackTeamQuery = Compiled(uncompiledAllForSlackTeamQuery _)

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]] = {
    dataService.run(allForSlackTeamQuery(slackTeamId).result)
  }

  def uncompiledAllSinceQuery(when: Rep[OffsetDateTime]) = {
    all.filter(_.createdAt >= when)
  }
  val allSinceQuery = Compiled(uncompiledAllSinceQuery _)

  def allSince(when: OffsetDateTime): Future[Seq[SlackBotProfile]] = {
    dataService.run(allSinceQuery(when).result)
  }

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile] = {
    val query = findQuery(userId)
    val action = query.result.headOption.flatMap {
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
        val newProfile = SlackBotProfile(userId, team.id, slackTeamId, token, OffsetDateTime.now)
        (all += newProfile).map { _ => newProfile }
      }
    }
    dataService.run(action)
  }


}
