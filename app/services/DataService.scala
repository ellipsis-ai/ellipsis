package services

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.logintoken.LoginTokenService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.team.TeamService
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val users: UserService
  val loginTokens: LoginTokenService
  val linkedAccounts: LinkedAccountService
  val teams: TeamService
  val apiTokens: APITokenService

  def run[T](action: DBIO[T]): Future[T]
  def runNow[T](action: DBIO[T]): T
}
