package models.accounts.oauth1tokenshare

import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.oauth1application.OAuth1Application
import models.accounts.user.User
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth1TokenSharesTable(tag: Tag) extends Table[OAuth1TokenShare](tag, "oauth1_token_shares") {
  def applicationId = column[String]("application_id")
  def userId = column[String]("user_id")
  def teamId = column[String]("team_id")

  def * = (applicationId, userId, teamId) <>
    ((OAuth1TokenShare.apply _).tupled, OAuth1TokenShare.unapply _)

}

class OAuth1TokenShareServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService],
                                               implicit val ec: ExecutionContext
                                             ) extends OAuth1TokenShareService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[OAuth1TokenSharesTable]

  def uncompiledFindForQuery(teamId: Rep[String], applicationId: Rep[String]) = {
    all.filter(_.teamId === teamId).filter(_.applicationId === applicationId)
  }
  val findForQuery = Compiled(uncompiledFindForQuery _)

  def uncompiledAllForTeamIdQuery(teamId: Rep[String]) = {
    all.filter(_.teamId === teamId)
  }
  val allForTeamIdQuery = Compiled(uncompiledAllForTeamIdQuery _)

  def allForAction(teamId: String): DBIO[Seq[OAuth1TokenShare]] = {
    allForTeamIdQuery(teamId).result
  }

  def ensureFor(user: User, application: OAuth1Application): Future[OAuth1TokenShare] = {
    val newInstance = OAuth1TokenShare(application.id, user.id, user.teamId)
    val action = (for {
      _ <- findForQuery(user.teamId, application.id).delete
      _ <- all += newInstance
    } yield newInstance).transactionally
    dataService.run(action)
  }

  def findFor(team: Team, application: OAuth1Application): Future[Option[OAuth1TokenShare]] = {
    val action = findForQuery(team.id, application.id).result.map(_.headOption)
    dataService.run(action)
  }

}
