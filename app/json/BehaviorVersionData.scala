package json

import models.team.Team
import models.accounts.user.User
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import Formatting._
import services.DataService

import scala.concurrent.Future

case class BehaviorVersionData(
                                teamId: String,
                                behaviorId: Option[String],
                                functionBody: String,
                                responseTemplate: String,
                                params: Seq[BehaviorParameterData],
                                triggers: Seq[BehaviorTriggerData],
                                config: BehaviorConfig,
                                importedId: Option[String],
                                githubUrl: Option[String],
                                knownEnvVarsUsed: Seq[String],
                                createdAt: Option[DateTime]
                                ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def copyForTeam(team: Team): BehaviorVersionData = {
    copy(teamId = team.id)
  }
}

object BehaviorVersionData {

  private def extractFunctionBodyFrom(function: String): String = {
    """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
  }

  def buildFor(teamId: String,
            behaviorId: Option[String],
            functionBody: String,
            responseTemplate: String,
            params: Seq[BehaviorParameterData],
            triggers: Seq[BehaviorTriggerData],
            config: BehaviorConfig,
            importedId: Option[String],
            githubUrl: Option[String],
            createdAt: Option[DateTime],
            dataService: DataService
              ): BehaviorVersionData = {

    val knownEnvVarsUsed = config.knownEnvVarsUsed ++ dataService.behaviorVersions.environmentVariablesUsedInCode(functionBody)

    BehaviorVersionData(
    teamId,
    behaviorId,
    functionBody,
    responseTemplate,
    params,
    triggers,
    config,
    importedId,
    githubUrl,
    knownEnvVarsUsed,
    createdAt
    )
  }

  def fromStrings(
                   teamId: String,
                   function: String,
                   response: String,
                   params: String,
                   triggers: String,
                   config: String,
                   maybeGithubUrl: Option[String],
                   dataService: DataService
                   ): BehaviorVersionData = {
    BehaviorVersionData.buildFor(
      teamId,
      None,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[BehaviorParameterData]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      Json.parse(config).validate[BehaviorConfig].get,
      importedId = None,
      maybeGithubUrl,
      createdAt = None,
      dataService
    )
  }

  def maybeFor(behaviorId: String, user: User, dataService: DataService, maybePublishedId: Option[String] = None): Future[Option[BehaviorVersionData]] = {
    for {
      maybeBehavior <- dataService.behaviors.find(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        dataService.behaviors.maybeCurrentVersionFor(behavior)
      }.getOrElse(Future.successful(None))
      maybeParameters <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorParameters.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.messageTriggers.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeAWSConfig <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.awsConfigs.maybeFor(behaviorVersion)
      }.getOrElse(Future.successful(None))
      maybeRequiredOAuth2ApiConfigs <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        behavior <- maybeBehavior
        behaviorVersion <- maybeBehaviorVersion
        params <- maybeParameters
        triggers <- maybeTriggers
        requiredOAuth2ApiConfigs <- maybeRequiredOAuth2ApiConfigs
      } yield {
        val maybeAWSConfigData = maybeAWSConfig.map { config =>
          AWSConfigData(config.maybeAccessKeyName, config.maybeSecretKeyName, config.maybeRegionName)
        }
        val maybeRequiredOAuth2ApiConfigData = maybeRequiredOAuth2ApiConfigs.map { requiredOAuth2ApiConfigs =>
          requiredOAuth2ApiConfigs.map(ea => RequiredOAuth2ApiConfigData.from(ea))
        }
        BehaviorVersionData.buildFor(
          behaviorVersion.team.id,
          Some(behavior.id),
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map { ea =>
            BehaviorParameterData(ea.name, BehaviorParameterTypeData(ea.paramType.name), ea.question)
          },
          triggers.map(ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          BehaviorConfig(maybePublishedId, maybeAWSConfigData, maybeRequiredOAuth2ApiConfigData),
          behavior.maybeImportedId,
          githubUrl = None,
          Some(behaviorVersion.createdAt),
          dataService
        )
      }
    }
  }
}
