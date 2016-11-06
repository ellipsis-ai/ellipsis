package services

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.slack.botprofile.SlackBotProfileService
import models.accounts.slack.profile.SlackProfileService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.behaviors.behavior.BehaviorService
import models.behaviors.behaviorparameter.BehaviorParameterService
import models.behaviors.behaviorversion.BehaviorVersionService
import models.behaviors.config.awsconfig.AWSConfigService
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfigService
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValueService
import models.behaviors.conversations.conversation.ConversationService
import models.behaviors.invocationlogentry.InvocationLogEntryService
import models.behaviors.scheduledmessage.ScheduledMessageService
import models.behaviors.triggers.messagetrigger.MessageTriggerService
import models.environmentvariable.{TeamEnvironmentVariableService, UserEnvironmentVariableService}
import models.behaviors.invocationtoken.InvocationTokenService
import models.team.TeamService
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val users: UserService
  val loginTokens: LoginTokenService
  val linkedAccounts: LinkedAccountService
  val teams: TeamService
  val apiTokens: APITokenService
  val teamEnvironmentVariables: TeamEnvironmentVariableService
  val userEnvironmentVariables: UserEnvironmentVariableService
  val invocationTokens: InvocationTokenService
  val linkedOAuth2Tokens: LinkedOAuth2TokenService
  val oauth2Apis: OAuth2ApiService
  val oauth2Applications: OAuth2ApplicationService
  val slackProfiles: SlackProfileService
  val slackBotProfiles: SlackBotProfileService
  val oauth2Tokens: OAuth2TokenService
  val behaviors: BehaviorService
  val behaviorVersions: BehaviorVersionService
  val behaviorParameters: BehaviorParameterService
  val messageTriggers: MessageTriggerService
  val awsConfigs: AWSConfigService
  val requiredOAuth2ApiConfigs: RequiredOAuth2ApiConfigService
  val conversations: ConversationService
  val collectedParameterValues: CollectedParameterValueService
  val scheduledMessages: ScheduledMessageService
  val invocationLogEntries: InvocationLogEntryService

  def run[T](action: DBIO[T]): Future[T]
  def runNow[T](action: DBIO[T]): T
  def runNow[T](future: Future[T]): T
}
