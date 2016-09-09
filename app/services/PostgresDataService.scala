package services

import javax.inject._

import models._
import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.environmentvariable.EnvironmentVariableService
import models.invocationtoken.InvocationTokenService
import models.team.TeamService
import slick.dbio.DBIO

import scala.concurrent.Future

@Singleton
class PostgresDataService @Inject() (
                                      val models: Models,
                                      val usersProvider: Provider[UserService],
                                      val loginTokensProvider: Provider[LoginTokenService],
                                      val linkedAccountsProvider: Provider[LinkedAccountService],
                                      val teamsProvider: Provider[TeamService],
                                      val apiTokensProvider: Provider[APITokenService],
                                      val environmentVariablesProvider: Provider[EnvironmentVariableService],
                                      val invocationTokensProvider: Provider[InvocationTokenService],
                                      val linkedOAuth2TokensProvider: Provider[LinkedOAuth2TokenService]
                            ) extends DataService {

  val users = usersProvider.get
  val loginTokens = loginTokensProvider.get
  val linkedAccounts = linkedAccountsProvider.get
  val teams = teamsProvider.get
  val apiTokens = apiTokensProvider.get
  val environmentVariables = environmentVariablesProvider.get
  val invocationTokens = invocationTokensProvider.get
  val linkedOAuth2Tokens = linkedOAuth2TokensProvider.get

  def run[T](action: DBIO[T]): Future[T] = models.run(action)
  def runNow[T](action: DBIO[T]): T = models.runNow(action)
}
