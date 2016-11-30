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
                                groupId: Option[String],
                                groupName: Option[String],
                                groupDescription: Option[String],
                                behaviorId: Option[String],
                                description: Option[String],
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

  def copyWithAttachedDataTypesFrom(dataTypes: Seq[BehaviorVersionData]): BehaviorVersionData = {
    copy(params = params.map(_.copyWithAttachedDataTypeFrom(dataTypes)))
  }

  lazy val isDataType: Boolean = config.dataTypeName.isDefined

  lazy val maybeFirstTrigger: Option[String] = triggers.filter(!_.isRegex).map(_.text.toLowerCase).sorted.headOption
}

object BehaviorVersionData {

  def extractFunctionBodyFrom(function: String): String = {
    """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
  }

  def buildFor(
                teamId: String,
                groupId: Option[String],
                groupName: Option[String],
                groupDescription: Option[String],
                behaviorId: Option[String],
                description: Option[String],
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

    val knownEnvVarsUsed =
      config.knownEnvVarsUsed ++
        dataService.teamEnvironmentVariables.lookForInCode(functionBody) ++
        dataService.userEnvironmentVariables.lookForInCode(functionBody)

    BehaviorVersionData(
      teamId,
      groupId,
      groupName,
      groupDescription,
      behaviorId,
      description,
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
                   maybeDescription: Option[String],
                   function: String,
                   response: String,
                   params: String,
                   triggers: String,
                   configString: String,
                   maybeGithubUrl: Option[String],
                   dataService: DataService
                   ): BehaviorVersionData = {
    val config = Json.parse(configString).validate[BehaviorConfig].get
    BehaviorVersionData.buildFor(
      teamId,
      None,
      None,
      None,
      None,
      maybeDescription,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[BehaviorParameterData]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      config,
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
      paramTypes <- Future.successful(maybeParameters.map { params =>
        params.map(_.paramType).distinct
      }.getOrElse(Seq()))
      paramTypeDataByParamTypes <- Future.sequence(paramTypes.map { paramType =>
        BehaviorParameterTypeData.from(paramType, dataService).map { data =>
          (paramType, data)
        }
      }).map(_.toMap)
      maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.messageTriggers.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeAWSConfig <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.awsConfigs.maybeFor(behaviorVersion)
      }.getOrElse(Future.successful(None))
      maybeRequiredOAuth2ApiConfigs <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeRequiredSimpleTokenApis <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.requiredSimpleTokenApis.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        behavior <- maybeBehavior
        behaviorVersion <- maybeBehaviorVersion
        params <- maybeParameters
        triggers <- maybeTriggers
        requiredOAuth2ApiConfigs <- maybeRequiredOAuth2ApiConfigs
        requiredSimpleTokenApis <- maybeRequiredSimpleTokenApis
      } yield {
        val maybeAWSConfigData = maybeAWSConfig.map { config =>
          AWSConfigData(config.maybeAccessKeyName, config.maybeSecretKeyName, config.maybeRegionName)
        }
        val requiredOAuth2ApiConfigData = requiredOAuth2ApiConfigs.map(ea => RequiredOAuth2ApiConfigData.from(ea))
        val requiredSimpleTokenApiData = requiredSimpleTokenApis.map(ea => RequiredSimpleTokenApiData.from(ea))
        val config = BehaviorConfig(maybePublishedId, maybeAWSConfigData, Some(requiredOAuth2ApiConfigData), Some(requiredSimpleTokenApiData), Some(behaviorVersion.forcePrivateResponse), behavior.maybeDataTypeName)
        BehaviorVersionData.buildFor(
          behaviorVersion.team.id,
          behavior.maybeGroup.map(_.id),
          behavior.maybeGroup.map(_.name),
          behavior.maybeGroup.flatMap(_.maybeDescription),
          Some(behavior.id),
          behaviorVersion.maybeDescription,
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map { ea =>
            BehaviorParameterData(
              ea.name,
              paramTypeDataByParamTypes.get(ea.paramType),
              ea.question,
              Some(ea.input.isSavedForTeam),
              Some(ea.input.isSavedForUser),
              Some(ea.input.id),
              ea.input.maybeBehaviorGroup.map(_.id)
            )
          },
          triggers.sortBy(ea => (ea.sortRank, ea.pattern)).map(ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          config,
          behavior.maybeImportedId,
          githubUrl = None,
          Some(behaviorVersion.createdAt),
          dataService
        )
      }
    }
  }
}
