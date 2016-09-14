package services

import javax.inject._

import models._
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
import models.bots.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfigService
import models.bots.conversations.conversation.ConversationService
import models.bots.triggers.messagetrigger.MessageTriggerService
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
                                      val linkedOAuth2TokensProvider: Provider[LinkedOAuth2TokenService],
                                      val oauth2ApisProvider: Provider[OAuth2ApiService],
                                      val oauth2ApplicationsProvider: Provider[OAuth2ApplicationService],
                                      val slackProfilesProvider: Provider[SlackProfileService],
                                      val slackBotProfilesProvider: Provider[SlackBotProfileService],
                                      val oauth2TokensProvider: Provider[OAuth2TokenService],
                                      val behaviorsProvider: Provider[BehaviorService],
                                      val behaviorVersionsProvider: Provider[BehaviorVersionService],
                                      val behaviorParametersProvider: Provider[BehaviorParameterService],
                                      val messageTriggersProvider: Provider[MessageTriggerService],
                                      val awsConfigsProvider: Provider[AWSConfigService],
                                      val requiredOAuth2ApiConfigsProvider: Provider[RequiredOAuth2ApiConfigService],
                                      val conversationsProvider: Provider[ConversationService]
                            ) extends DataService {

  val users = usersProvider.get
  val loginTokens = loginTokensProvider.get
  val linkedAccounts = linkedAccountsProvider.get
  val teams = teamsProvider.get
  val apiTokens = apiTokensProvider.get
  val environmentVariables = environmentVariablesProvider.get
  val invocationTokens = invocationTokensProvider.get
  val linkedOAuth2Tokens = linkedOAuth2TokensProvider.get
  val oauth2Apis = oauth2ApisProvider.get
  val oauth2Applications = oauth2ApplicationsProvider.get
  val slackProfiles = slackProfilesProvider.get
  val slackBotProfiles = slackBotProfilesProvider.get
  val oauth2Tokens = oauth2TokensProvider.get
  val behaviors = behaviorsProvider.get
  val behaviorVersions = behaviorVersionsProvider.get
  val behaviorParameters = behaviorParametersProvider.get
  val messageTriggers = messageTriggersProvider.get
  val awsConfigs = awsConfigsProvider.get
  val requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsProvider.get
  val conversations = conversationsProvider.get

  def run[T](action: DBIO[T]): Future[T] = models.run(action)
  def runNow[T](action: DBIO[T]): T = models.runNow(action)
  def runNow[T](future: Future[T]): T = models.runNow(future)
}
