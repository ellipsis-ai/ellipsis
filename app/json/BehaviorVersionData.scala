package json

import java.time.OffsetDateTime

import models.team.Team
import models.accounts.user.User
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import Formatting._
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.Future

case class BehaviorVersionData(
                                id: Option[String],
                                teamId: String,
                                groupId: Option[String],
                                behaviorId: Option[String],
                                isNewBehavior: Option[Boolean],
                                name: Option[String],
                                description: Option[String],
                                functionBody: String,
                                responseTemplate: String,
                                params: Seq[BehaviorParameterData],
                                triggers: Seq[BehaviorTriggerData],
                                config: BehaviorConfig,
                                exportId: Option[String],
                                githubUrl: Option[String],
                                knownEnvVarsUsed: Seq[String],
                                createdAt: Option[OffsetDateTime]
                                ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def copyForTeam(team: Team): BehaviorVersionData = {
    copy(teamId = team.id)
  }

  def copyWithIdsEnsuredFor(group: BehaviorGroup): BehaviorVersionData = {
    copy(
      teamId = group.team.id,
      groupId = Some(group.id),
      behaviorId = behaviorId.orElse(Some(IDs.next))
    )
  }

  def copyWithInputIdsFrom(inputs: Seq[InputData]): BehaviorVersionData = {
    val paramsWithNewInputIds = params.map { param =>
      val maybeNewId = param.inputExportId.flatMap { exportId =>
        inputs.find(_.exportId.contains(exportId)).flatMap(_.id)
      }
      param.copy(inputId = maybeNewId)
    }
    copy(params = paramsWithNewInputIds)
  }

  def copyForClone: BehaviorVersionData = {
    copy(
      id = None,
      behaviorId = Some(IDs.next),
      exportId = None
    )
  }

  def copyWithNewIdIn(oldToNewIdMapping: collection.mutable.Map[String, String]): BehaviorVersionData = {
    val newId = IDs.next
    val maybeOldID = id
    maybeOldID.foreach { oldId => oldToNewIdMapping.put(oldId, newId) }
    copy(id = Some(newId))
  }

  def copyWithInputIdsIn(
                          inputs: Seq[InputData],
                          oldToNewIdMapping: collection.mutable.Map[String, String]
                        ): BehaviorVersionData = {
    val newParams = params.map { param =>
      val maybeNewId = param.inputId.flatMap { inputId =>
        oldToNewIdMapping.get(inputId)
      }
      (for {
        newId <- maybeNewId
        input <- inputs.find(_.id.contains(newId))
      } yield {
        param.copy(paramType = input.paramType, inputId = input.id, inputExportId = input.exportId)
      }).getOrElse(param)
    }
    copy(params = newParams)
  }

  def copyWithEnsuredInputIds: BehaviorVersionData = {
    val paramsWithEnsuredInputIds = params.map { param =>
      if (param.inputId.isDefined) {
        param
      } else {
        param.copy(inputId = Some(IDs.next))
      }
    }
    copy(params = paramsWithEnsuredInputIds)
  }

  lazy val isDataType: Boolean = config.dataTypeName.isDefined

  lazy val maybeFirstTrigger: Option[String] = triggers.filterNot(_.isRegex).map(_.text.toLowerCase).sorted.headOption
}

object BehaviorVersionData {

  def extractFunctionBodyFrom(function: String): String = {
    """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
  }

  def buildFor(
                id: Option[String],
                teamId: String,
                groupId: Option[String],
                behaviorId: Option[String],
                isNewBehavior: Boolean,
                description: Option[String],
                functionBody: String,
                responseTemplate: String,
                params: Seq[BehaviorParameterData],
                triggers: Seq[BehaviorTriggerData],
                config: BehaviorConfig,
                exportId: Option[String],
                githubUrl: Option[String],
                createdAt: Option[OffsetDateTime],
                dataService: DataService
              ): BehaviorVersionData = {

    val knownEnvVarsUsed =
      config.knownEnvVarsUsed ++
        dataService.teamEnvironmentVariables.lookForInCode(functionBody) ++
        dataService.userEnvironmentVariables.lookForInCode(functionBody)


    BehaviorVersionData(
      id,
      teamId,
      groupId,
      behaviorId,
      Some(isNewBehavior),
      config.name,
      description,
      functionBody,
      responseTemplate,
      params,
      triggers.sorted,
      config,
      exportId,
      githubUrl,
      knownEnvVarsUsed,
      createdAt
    )
  }

  def newUnsavedFor(teamId: String, maybeGroupId: Option[String], isDataType: Boolean, dataService: DataService): BehaviorVersionData = {
    buildFor(
      None,
      teamId,
      maybeGroupId,
      Some(IDs.next),
      isNewBehavior = true,
      None,
      "",
      "",
      Seq(),
      Seq(),
      BehaviorConfig(None, None, None, None, None, None, if (isDataType) { Some("") } else { None }),
      None,
      None,
      None,
      dataService
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
      None,
      teamId,
      None,
      None,
      isNewBehavior = false,
      maybeDescription,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[BehaviorParameterData]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      config,
      exportId = None,
      maybeGithubUrl,
      createdAt = None,
      dataService
    )
  }

  def maybeFor(
                behaviorId: String,
                user: User,
                dataService: DataService,
                maybeGroupVersion: Option[BehaviorGroupVersion],
                maybeExportId: Option[String] = None
              ): Future[Option[BehaviorVersionData]] = {
    for {
      maybeBehavior <- dataService.behaviors.find(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        maybeGroupVersion.map { groupVersion =>
          dataService.behaviorVersions.findFor(behavior, groupVersion)
        }.getOrElse(dataService.behaviors.maybeCurrentVersionFor(behavior))
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
        val config = BehaviorConfig(maybeExportId, behaviorVersion.maybeName, maybeAWSConfigData, Some(requiredOAuth2ApiConfigData), Some(requiredSimpleTokenApiData), Some(behaviorVersion.forcePrivateResponse), behavior.maybeDataTypeName)
        BehaviorVersionData.buildFor(
          Some(behaviorVersion.id),
          behaviorVersion.team.id,
          behavior.maybeGroup.map(_.id),
          Some(behavior.id),
          isNewBehavior = false,
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
              ea.input.maybeExportId,
              Some(ea.input.behaviorGroupVersion.id)
            )
          },
          triggers.map(ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          config,
          behavior.maybeExportId,
          githubUrl = None,
          Some(behaviorVersion.createdAt),
          dataService
        )
      }
    }
  }
}
