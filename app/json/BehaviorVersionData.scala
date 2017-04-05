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
                                behaviorId: Option[String],
                                isNewBehavior: Option[Boolean],
                                name: Option[String],
                                description: Option[String],
                                functionBody: String,
                                responseTemplate: String,
                                inputIds: Seq[String],
                                triggers: Seq[BehaviorTriggerData],
                                config: BehaviorConfig,
                                exportId: Option[String],
                                githubUrl: Option[String],
                                knownEnvVarsUsed: Seq[String],
                                createdAt: Option[OffsetDateTime]
                                ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def maybeExportName: Option[String] = {
    name.orElse(exportId)
  }

  def copyForImportableForTeam(team: Team, inputsData: Seq[InputData]): BehaviorVersionData = {
    copy(
      id = Some(IDs.next),
      behaviorId = Some(IDs.next),
      teamId = team.id,
      inputIds = inputIds.flatMap { id => inputsData.find(_.exportId.contains(id)).flatMap(_.inputId) }
    )
  }

  def copyWithIdsEnsuredForUpdateOf(groupData: BehaviorGroupData, inputsData: Seq[InputData]): BehaviorVersionData = {
    val maybeExisting = groupData.behaviorVersions.find(_.exportId == exportId)
    copy(
      behaviorId = maybeExisting.flatMap(_.behaviorId).orElse(Some(IDs.next)),
      inputIds = inputIds.flatMap { id => inputsData.find(_.exportId.contains(id)).flatMap(_.inputId) }
    )
  }

  def copyForClone: BehaviorVersionData = {
    copy(
      id = Some(IDs.next),
      behaviorId = Some(IDs.next),
      exportId = None,
      name = name.map(n => s"Copy of $n"),
      isNewBehavior = Some(true)
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
                isNewBehavior: Boolean,
                description: Option[String],
                functionBody: String,
                responseTemplate: String,
                inputIds: Seq[String],
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
      behaviorId,
      Some(isNewBehavior),
      config.name,
      description,
      functionBody,
      responseTemplate,
      inputIds,
      triggers.sorted,
      config,
      exportId,
      githubUrl,
      knownEnvVarsUsed,
      createdAt
    )
  }

  def newUnsavedFor(teamId: String, isDataType: Boolean, dataService: DataService): BehaviorVersionData = {
    buildFor(
      Some(IDs.next),
      teamId,
      Some(IDs.next),
      isNewBehavior = true,
      None,
      "",
      "",
      Seq(),
      Seq(BehaviorTriggerData("", requiresMention = true, isRegex = false, caseSensitive = false)),
      BehaviorConfig(None, None, None, None, isDataType),
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
      isNewBehavior = false,
      maybeDescription,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[String]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      config,
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
        BehaviorVersionData.buildFor(
          Some(behaviorVersion.id),
          behaviorVersion.team.id,
          Some(behavior.id),
          isNewBehavior = false,
          behaviorVersion.maybeDescription,
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map(_.input.inputId),
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
