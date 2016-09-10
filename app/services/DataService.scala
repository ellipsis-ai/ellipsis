package services

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.slack.profile.SlackProfileService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.environmentvariable.EnvironmentVariableService
import models.invocationtoken.InvocationTokenService
import models.team.TeamService
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val users: UserService
  val loginTokens: LoginTokenService
  val linkedAccounts: LinkedAccountService
  val teams: TeamService
  val apiTokens: APITokenService
  val environmentVariables: EnvironmentVariableService
  val invocationTokens: InvocationTokenService
  val linkedOAuth2Tokens: LinkedOAuth2TokenService
  val oauth2Apis: OAuth2ApiService
  val oauth2Applications: OAuth2ApplicationService
  val slackProfiles: SlackProfileService
  val oauth2Tokens: OAuth2TokenService

  def run[T](action: DBIO[T]): Future[T]
  def runNow[T](action: DBIO[T]): T
}
