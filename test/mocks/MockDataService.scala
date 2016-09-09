package mocks

import javax.inject.Singleton

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.environmentvariable.EnvironmentVariableService
import models.invocationtoken.InvocationTokenService
import models.team.TeamService
import org.scalatest.mock.MockitoSugar
import slick.dbio.DBIO
import services.DataService

import scala.concurrent.Future

@Singleton
class MockDataService extends DataService with MockitoSugar {

  val users: UserService = mock[UserService]
  val loginTokens: LoginTokenService = mock[LoginTokenService]
  val linkedAccounts: LinkedAccountService = mock[LinkedAccountService]
  val teams: TeamService = mock[TeamService]
  val apiTokens: APITokenService = mock[APITokenService]
  val environmentVariables: EnvironmentVariableService = mock[EnvironmentVariableService]
  val invocationTokens: InvocationTokenService = mock[InvocationTokenService]
  val linkedOAuth2Tokens: LinkedOAuth2TokenService = mock[LinkedOAuth2TokenService]

  def run[T](action: DBIO[T]): Future[T] = throw new Exception("Don't call me")
  def runNow[T](action: DBIO[T]): T = throw new Exception("Don't call me")

}
