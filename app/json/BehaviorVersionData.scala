package json

import java.time.OffsetDateTime

import models.team.Team
import models.accounts.user.User
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import Formatting._
import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.Future

case class BehaviorVersionData(
                                id: Option[String],
                                teamId: String,
                                behaviorId: Option[String],
                                groupId: Option[String],
                                isNew: Option[Boolean],
                                name: Option[String],
                                description: Option[String],
                                functionBody: String,
                                responseTemplate: String,
                                inputIds: Seq[String],
                                triggers: Seq[BehaviorTriggerData],
                                config: BehaviorConfig,
                                dataTypeConfig: Option[DataTypeConfigData],
                                exportId: Option[String],
                                githubUrl: Option[String],
                                knownEnvVarsUsed: Seq[String],
                                createdAt: Option[OffsetDateTime]
                                ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def maybeExportName: Option[String] = {
    name.orElse(exportId)
  }

  def copyForImportableForTeam(team: Team, inputsData: Seq[InputData], maybeExistingGroupData: Option[BehaviorGroupData]): BehaviorVersionData = {
    val maybeExisting = maybeExistingGroupData.flatMap { data =>
      data.behaviorVersions.find(_.exportId == exportId)
    }
    copy(
      id = Some(IDs.next),
      behaviorId = maybeExisting.flatMap(_.behaviorId).orElse(Some(IDs.next)),
      teamId = team.id,
      inputIds = inputIds.flatMap { id => inputsData.find(_.exportId.contains(id)).flatMap(_.inputId) }
    )
  }

  def copyForClone: BehaviorVersionData = {
    copy(
      id = Some(IDs.next),
      behaviorId = Some(IDs.next),
      exportId = None,
      name = name.map(n => s"Copy of $n"),
      isNew = Some(true)
    )
  }

  def copyWithNewIdIn(oldToNewIdMapping: collection.mutable.Map[String, String]): BehaviorVersionData = {
    val newId = IDs.next
    val maybeOldID = id
    maybeOldID.foreach { oldId => oldToNewIdMapping.put(oldId, newId) }
    copy(id = Some(newId))
  }

  lazy val isDataType: Boolean = config.isDataType

  lazy val maybeFirstTrigger: Option[String] = triggers.filterNot(_.isRegex).map(_.text.toLowerCase).sorted.headOption

  def maybeFunction(dataService: DataService): Future[Option[String]] = {
    id.map { behaviorVersionId =>
      dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId).flatMap { maybeBehaviorVersion =>
        maybeBehaviorVersion.map { behaviorVersion =>
          dataService.behaviorVersions.maybeFunctionFor(behaviorVersion)
        }.getOrElse(Future.successful(None))
      }
    }.getOrElse(Future.successful(None))
  }
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
                behaviorId: Option[String],
                groupId: Option[String],
                isNew: Boolean,
                description: Option[String],
                functionBody: String,
                responseTemplate: String,
                inputIds: Seq[String],
                triggers: Seq[BehaviorTriggerData],
                config: BehaviorConfig,
                dataTypeConfig: Option[DataTypeConfigData],
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
      behaviorId,
      groupId,
      Some(isNew),
      config.name,
      description,
      functionBody,
      responseTemplate,
      inputIds,
      triggers.sorted,
      config,
      dataTypeConfig,
      exportId,
      githubUrl,
      knownEnvVarsUsed,
      createdAt
    )
  }

  def newUnsavedFor(teamId: String, isDataType: Boolean, dataService: DataService): BehaviorVersionData = {
    val maybeDataTypeConfig = if (isDataType) { Some(DataTypeConfigData(Seq())) } else { None }
    buildFor(
      Some(IDs.next),
      teamId,
      Some(IDs.next),
      None,
      isNew = true,
      None,
      "",
      "",
      Seq(),
      Seq(BehaviorTriggerData("", requiresMention = true, isRegex = false, caseSensitive = false)),
      BehaviorConfig(None, None, None, None, isDataType),
      maybeDataTypeConfig,
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
                   maybeDataTypeConfigString: Option[String],
                   maybeGithubUrl: Option[String],
                   dataService: DataService
                   ): BehaviorVersionData = {
    val config = Json.parse(configString).validate[BehaviorConfig].get
    BehaviorVersionData.buildFor(
      None,
      teamId,
      None,
      None,
      isNew = false,
      maybeDescription,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[String]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      config,
      maybeDataTypeConfigString.map { cfg =>
        Json.parse(cfg).validate[DataTypeConfigData].get
      },
      config.exportId,
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
                maybeExportId: Option[String]
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
      maybeDataTypeConfig <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.dataTypeConfigs.maybeFor(behaviorVersion)
      }.getOrElse(Future.successful(None))
      maybeDataTypeConfigData <- maybeDataTypeConfig.map { config =>
        DataTypeConfigData.forConfig(config, dataService).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        behavior <- maybeBehavior
        behaviorVersion <- maybeBehaviorVersion
        params <- maybeParameters
        triggers <- maybeTriggers
      } yield {
        val maybeAWSConfigData = maybeAWSConfig.map { config =>
          AWSConfigData(config.maybeAccessKeyName, config.maybeSecretKeyName, config.maybeRegionName)
        }
        val config = BehaviorConfig(maybeExportId, behaviorVersion.maybeName, maybeAWSConfigData, Some(behaviorVersion.forcePrivateResponse), behavior.isDataType)
        val maybeEnsuredDataTypeConfigData = maybeDataTypeConfigData.orElse {
          if (behavior.isDataType) {
            Some(DataTypeConfigData(Seq()))
          } else {
            None
          }
        }
        BehaviorVersionData.buildFor(
          Some(behaviorVersion.id),
          behaviorVersion.team.id,
          Some(behavior.id),
          maybeGroupVersion.map(_.group.id),
          isNew = false,
          behaviorVersion.maybeDescription,
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map(_.input.inputId),
          triggers.map(ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          config,
          maybeEnsuredDataTypeConfigData,
          behavior.maybeExportId,
          githubUrl = None,
          Some(behaviorVersion.createdAt),
          dataService
        )
      }
    }
  }
}
