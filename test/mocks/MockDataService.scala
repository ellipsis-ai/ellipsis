package mocks

import javax.inject.Singleton

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
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

  val users = mock[UserService]
  val loginTokens = mock[LoginTokenService]
  val linkedAccounts = mock[LinkedAccountService]
  val teams = mock[TeamService]
  val apiTokens = mock[APITokenService]
  val environmentVariables = mock[EnvironmentVariableService]
  val invocationTokens = mock[InvocationTokenService]
  val linkedOAuth2Tokens = mock[LinkedOAuth2TokenService]
  val oauth2Apis = mock[OAuth2ApiService]
  val oauth2Applications = mock[OAuth2ApplicationService]

  def run[T](action: DBIO[T]): Future[T] = throw new Exception("Don't call me")
  def runNow[T](action: DBIO[T]): T = throw new Exception("Don't call me")

}
