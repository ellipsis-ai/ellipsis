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
                                dataType: Option[BehaviorBackedDataTypeData],
                                createdAt: Option[DateTime]
                                ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def copyForTeam(team: Team): BehaviorVersionData = {
    copy(teamId = team.id)
  }
}

object BehaviorVersionData {

  def extractFunctionBodyFrom(function: String): String = {
    """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
  }

  def buildFor(
                teamId: String,
                behaviorId: Option[String],
                functionBody: String,
                responseTemplate: String,
                params: Seq[BehaviorParameterData],
                triggers: Seq[BehaviorTriggerData],
                config: BehaviorConfig,
                importedId: Option[String],
                githubUrl: Option[String],
                maybeDataType: Option[BehaviorBackedDataTypeData],
                createdAt: Option[DateTime],
                dataService: DataService
              ): BehaviorVersionData = {

    val knownEnvVarsUsed = config.knownEnvVarsUsed ++ dataService.environmentVariables.lookForInCode(functionBody)

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
      maybeDataType,
      createdAt
    )
  }

  def fromStrings(
                   teamId: String,
                   function: String,
                   response: String,
                   params: String,
                   triggers: String,
                   configString: String,
                   maybeGithubUrl: Option[String],
                   dataService: DataService
                   ): BehaviorVersionData = {
    val config = Json.parse(configString).validate[BehaviorConfig].get
    val maybeDataType = config.dataTypeName.map { name =>
      BehaviorBackedDataTypeData(None, Some(name))
    }
    BehaviorVersionData.buildFor(
      teamId,
      None,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[BehaviorParameterData]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      config,
      importedId = None,
      maybeGithubUrl,
      maybeDataType,
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
      dataTypes <- maybeBehavior.map { behavior =>
        dataService.behaviorBackedDataTypes.allFor(behavior.team)
      }.getOrElse(Future.successful(Seq()))
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
        val maybeDataType = dataTypes.find(_.behavior.id == behavior.id).map { dataType =>
          BehaviorBackedDataTypeData(Some(dataType.id), Some(dataType.name))
        }
        val config = BehaviorConfig(maybePublishedId, maybeAWSConfigData, maybeRequiredOAuth2ApiConfigData, Some(behaviorVersion.forcePrivateResponse), maybeDataType.flatMap(_.name))
        BehaviorVersionData.buildFor(
          behaviorVersion.team.id,
          Some(behavior.id),
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map { ea =>
            BehaviorParameterData(ea.name, Some(BehaviorParameterTypeData.from(ea.paramType)), ea.question)
          },
          triggers.sortBy(ea => (ea.sortRank, ea.pattern)).map(ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          config,
          behavior.maybeImportedId,
          githubUrl = None,
          maybeDataType,
          Some(behaviorVersion.createdAt),
          dataService
        )
      }
    }
  }
}
