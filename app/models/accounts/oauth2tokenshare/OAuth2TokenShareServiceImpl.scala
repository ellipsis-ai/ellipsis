package models.accounts.oauth2tokenshare

import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth2TokenSharesTable(tag: Tag) extends Table[OAuth2TokenShare](tag, "oauth2_token_shares") {
  def applicationId = column[String]("application_id")
  def userId = column[String]("user_id")
  def teamId = column[String]("team_id")

  def * = (applicationId, userId, teamId) <>
    ((OAuth2TokenShare.apply _).tupled, OAuth2TokenShare.unapply _)

}

class OAuth2TokenShareServiceImpl @Inject() (
                                              dataServiceProvider: Provider[DataService],
                                              implicit val ec: ExecutionContext
                                            ) extends OAuth2TokenShareService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[OAuth2TokenSharesTable]

  def uncompiledFindForQuery(teamId: Rep[String], applicationId: Rep[String]) = {
    all.filter(_.teamId === teamId).filter(_.applicationId === applicationId)
  }
  val findForQuery = Compiled(uncompiledFindForQuery _)

  def uncompiledAllForTeamIdQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForTeamIdQuery = Compiled(uncompiledAllForTeamIdQuery _)

  def allForAction(teamId: String): DBIO[Seq[OAuth2TokenShare]] = {
    allForTeamIdQuery(teamId).result
  }

  def ensureFor(user: User, application: OAuth2Application): Future[OAuth2TokenShare] = {
    val newInstance = OAuth2TokenShare(application.id, user.id, user.teamId)
    val action = (for {
      _ <- findForQuery(user.teamId, application.id).delete
      _ <- all += newInstance
    } yield newInstance).transactionally
    dataService.run(action)
  }

  def removeFor(user: User, application: OAuth2Application, maybeTeam: Option[Team]): Future[Unit] = {
    val action = findForQuery(maybeTeam.map(_.id).getOrElse(user.teamId), application.id).delete
    dataService.run(action).map(_ => {})
  }

  def findFor(team: Team, application: OAuth2Application): Future[Option[OAuth2TokenShare]] = {
    val action = findForQuery(team.id, application.id).result.map(_.headOption)
    dataService.run(action)
  }

}
