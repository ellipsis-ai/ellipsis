package mocks

import javax.inject.Singleton

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.slack.profile.SlackProfileService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.slack.botprofile.SlackBotProfileService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.bots.behavior.BehaviorService
import models.bots.behaviorparameter.BehaviorParameterService
import models.bots.behaviorversion.BehaviorVersionService
import models.bots.config.awsconfig.AWSConfigService
import models.bots.triggers.messagetrigger.MessageTriggerService
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
  val slackProfiles = mock[SlackProfileService]
  val slackBotProfiles = mock[SlackBotProfileService]
  val oauth2Tokens = mock[OAuth2TokenService]
  val behaviors = mock[BehaviorService]
  val behaviorVersions = mock[BehaviorVersionService]
  val behaviorParameters = mock[BehaviorParameterService]
  val messageTriggers = mock[MessageTriggerService]
  val awsConfigs = mock[AWSConfigService]

  private def dontCallMe = throw new Exception("Don't call me")

  def run[T](action: DBIO[T]): Future[T] = dontCallMe
  def runNow[T](action: DBIO[T]): T = dontCallMe
  def runNow[T](future: Future[T]): T = dontCallMe
}
