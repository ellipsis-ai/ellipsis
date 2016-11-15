package services

import javax.inject._

import models._
import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.linkedsimpletoken.LinkedSimpleTokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.slack.profile.SlackProfileService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.simpletokenapi.SimpleTokenApiService
import models.accounts.slack.botprofile.SlackBotProfileService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.behaviors.behavior.BehaviorService
import models.behaviors.behaviorparameter.BehaviorParameterService
import models.behaviors.behaviorversion.BehaviorVersionService
import models.behaviors.config.awsconfig.AWSConfigService
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfigService
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApiService
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValueService
import models.behaviors.conversations.conversation.ConversationService
import models.behaviors.input.InputService
import models.behaviors.invocationlogentry.InvocationLogEntryService
import models.behaviors.scheduledmessage.ScheduledMessageService
import models.behaviors.triggers.messagetrigger.MessageTriggerService
import models.environmentvariable.{TeamEnvironmentVariableService, UserEnvironmentVariableService}
import models.behaviors.invocationtoken.InvocationTokenService
import models.behaviors.savedanswer.SavedAnswerService
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
                                      val environmentVariablesProvider: Provider[TeamEnvironmentVariableService],
                                      val userEnvironmentVariablesProvider: Provider[UserEnvironmentVariableService],
                                      val invocationTokensProvider: Provider[InvocationTokenService],
                                      val linkedOAuth2TokensProvider: Provider[LinkedOAuth2TokenService],
                                      val linkedSimpleTokensProvider: Provider[LinkedSimpleTokenService],
                                      val oauth2ApisProvider: Provider[OAuth2ApiService],
                                      val oauth2ApplicationsProvider: Provider[OAuth2ApplicationService],
                                      val simpleTokenApisProvider: Provider[SimpleTokenApiService],
                                      val slackProfilesProvider: Provider[SlackProfileService],
                                      val slackBotProfilesProvider: Provider[SlackBotProfileService],
                                      val oauth2TokensProvider: Provider[OAuth2TokenService],
                                      val behaviorsProvider: Provider[BehaviorService],
                                      val behaviorVersionsProvider: Provider[BehaviorVersionService],
                                      val behaviorParametersProvider: Provider[BehaviorParameterService],
                                      val inputsProvider: Provider[InputService],
                                      val savedAnswersProvider: Provider[SavedAnswerService],
                                      val messageTriggersProvider: Provider[MessageTriggerService],
                                      val awsConfigsProvider: Provider[AWSConfigService],
                                      val requiredOAuth2ApiConfigsProvider: Provider[RequiredOAuth2ApiConfigService],
                                      val requiredSimpleTokenApiConfigsProvider: Provider[RequiredSimpleTokenApiService],
                                      val conversationsProvider: Provider[ConversationService],
                                      val collectedParameterValuesProvider: Provider[CollectedParameterValueService],
                                      val scheduledMessagesProvider: Provider[ScheduledMessageService],
                                      val invocationLogEntriesProvider: Provider[InvocationLogEntryService]
                            ) extends DataService {

  val users = usersProvider.get
  val loginTokens = loginTokensProvider.get
  val linkedAccounts = linkedAccountsProvider.get
  val teams = teamsProvider.get
  val apiTokens = apiTokensProvider.get
  val teamEnvironmentVariables = environmentVariablesProvider.get
  val userEnvironmentVariables = userEnvironmentVariablesProvider.get
  val invocationTokens = invocationTokensProvider.get
  val linkedOAuth2Tokens = linkedOAuth2TokensProvider.get
  val linkedSimpleTokens = linkedSimpleTokensProvider.get
  val oauth2Apis = oauth2ApisProvider.get
  val oauth2Applications = oauth2ApplicationsProvider.get
  val simpleTokenApis = simpleTokenApisProvider.get
  val slackProfiles = slackProfilesProvider.get
  val slackBotProfiles = slackBotProfilesProvider.get
  val oauth2Tokens = oauth2TokensProvider.get
  val behaviors = behaviorsProvider.get
  val behaviorVersions = behaviorVersionsProvider.get
  val behaviorParameters = behaviorParametersProvider.get
  val inputs = inputsProvider.get
  val savedAnswers = savedAnswersProvider.get
  val messageTriggers = messageTriggersProvider.get
  val awsConfigs = awsConfigsProvider.get
  val requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsProvider.get
  val requiredSimpleTokenApis = requiredSimpleTokenApiConfigsProvider.get
  val conversations = conversationsProvider.get
  val collectedParameterValues = collectedParameterValuesProvider.get
  val scheduledMessages = scheduledMessagesProvider.get
  val invocationLogEntries = invocationLogEntriesProvider.get

  def run[T](action: DBIO[T]): Future[T] = models.run(action)
  def runNow[T](action: DBIO[T]): T = models.runNow(action)
  def runNow[T](future: Future[T]): T = models.runNow(future)
}
