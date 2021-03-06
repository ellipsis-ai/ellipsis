package json

import java.time.OffsetDateTime

import json.Formatting._
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.datatypeconfig.BehaviorVersionForDataTypeSchema
import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import models.behaviors.triggers.MessageSent
import models.team.Team
import play.api.libs.json._
import services.DataService
import slick.dbio.DBIO
import utils.NameFormatter

import scala.concurrent.{ExecutionContext, Future}

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
                                exportId: Option[String],
                                createdAt: Option[OffsetDateTime]
                                ) extends BehaviorVersionForDataTypeSchema {

  lazy val typeName: String = name.getOrElse(GraphQLHelpers.fallbackTypeName)

  def dataTypeFieldsAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[DataTypeFieldForSchema]] = {
    DBIO.successful(config.dataTypeConfig.map(_.fields).getOrElse(Seq()))
  }

  def dataTypeFields(dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[DataTypeFieldForSchema]] = {
    dataService.run(dataTypeFieldsAction(dataService))
  }

  def maybeExportName: Option[String] = {
    name.filter(_.trim.nonEmpty).orElse(exportId)
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

  def copyForClone(newInputIds: Seq[String]): BehaviorVersionData = {
    copy(
      id = Some(IDs.next),
      behaviorId = Some(IDs.next),
      exportId = None,
      name = name.map(n => s"${n}Copy"),
      config = config.copyForClone,
      isNew = Some(true),
      inputIds = newInputIds
    )
  }

  def copyWithNewIdIn(oldToNewIdMapping: collection.mutable.Map[String, String]): BehaviorVersionData = {
    val newId = IDs.next
    val maybeOldID = id
    maybeOldID.foreach { oldId => oldToNewIdMapping.put(oldId, newId) }
    val nameToUse = name.map { n =>
      if (isDataType) {
        NameFormatter.formatDataTypeName(n)
      } else {
        n
      }
    }
    copy(id = Some(newId), name = nameToUse, config = config.copyForNewVersion)
  }

  def copyWithParamTypeIdsIn(oldToNewIdMapping: collection.mutable.Map[String, String]): BehaviorVersionData = {
    val maybeNewDataTypeConfig = config.dataTypeConfig.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping))
    copy(config = config.copy(dataTypeConfig = maybeNewDataTypeConfig))
  }

  lazy val isDataType: Boolean = config.isDataType

  lazy val isTest: Boolean = config.isTest.contains(true)

  lazy val maybeFirstTrigger: Option[String] = triggers.filterNot(_.isRegex).map(_.text.toLowerCase).sorted.headOption

  def maybeFunction(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[String]] = {
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

  def maybeDataTypeConfigFor(isDataType: Boolean, maybeName: Option[String]): Option[DataTypeConfigData] = {
    if (isDataType) {
      Some(DataTypeConfigData(Seq(), None))
    } else {
      None
    }
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
                exportId: Option[String],
                createdAt: Option[OffsetDateTime]
              ): BehaviorVersionData = {
    BehaviorVersionData(
      id,
      teamId,
      behaviorId,
      groupId,
      Some(isNew),
      config.name,
      description,
      functionBody.trim,
      responseTemplate,
      inputIds,
      triggers.sorted,
      config,
      exportId,
      createdAt
    )
  }

  def newUnsavedFor(teamId: String, isDataType: Boolean, isTest: Boolean, maybeName: Option[String]): BehaviorVersionData = {
    val maybeDataTypeConfig = maybeDataTypeConfigFor(isDataType, maybeName)
    buildFor(
      id = Some(IDs.next),
      teamId = teamId,
      behaviorId = Some(IDs.next),
      groupId = None,
      isNew = true,
      description = None,
      functionBody = "",
      responseTemplate = "",
      inputIds = Seq(),
      triggers = Seq(BehaviorTriggerData("", requiresMention = true, isRegex = false, caseSensitive = false, triggerType = MessageSent.toString)),
      config = BehaviorConfig(
        None,
        maybeName,
        BehaviorResponseTypeData.normal.id,
        None,
        isDataType = maybeDataTypeConfig.isDefined,
        Some(isTest),
        maybeDataTypeConfig
      ),
      exportId = None,
      createdAt = None
    )
  }

  def fromStrings(
                   teamId: String,
                   maybeDescription: Option[String],
                   function: String,
                   response: String,
                   params: String,
                   triggers: String,
                   configString: String
                   ): BehaviorVersionData = {
    val behaviorConfig = Json.parse(configString).validate[LegacyBehaviorConfigJson].get.toBehaviorConfig
    val triggerData = Json.parse(triggers).validate[Seq[LegacyBehaviorTriggerJson]].get.map(_.toBehaviorTriggerData)
    BehaviorVersionData.buildFor(
      id = None,
      teamId = teamId,
      behaviorId = None,
      groupId = None,
      isNew = false,
      description = maybeDescription,
      functionBody = extractFunctionBodyFrom(function),
      responseTemplate = response,
      inputIds = Json.parse(params).validate[Seq[String]].get,
      triggers = triggerData,
      config = behaviorConfig,
      exportId = behaviorConfig.exportId,
      createdAt = None
    )
  }

  def maybeForAction(
                      behaviorId: String,
                      user: User,
                      dataService: DataService,
                      maybeGroupVersion: Option[BehaviorGroupVersion],
                      maybeExportId: Option[String]
                    )(implicit ec: ExecutionContext): DBIO[Option[BehaviorVersionData]] = {
    for {
      maybeBehavior <- dataService.behaviors.findAction(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        maybeGroupVersion.map { groupVersion =>
          dataService.behaviorVersions.findForAction(behavior, groupVersion)
        }.getOrElse(dataService.behaviors.maybeCurrentVersionForAction(behavior))
      }.getOrElse(DBIO.successful(None))
      maybeParameters <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.behaviorParameters.allForAction(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      paramTypes <- DBIO.successful(maybeParameters.map { params =>
        params.map(_.paramType).distinct
      }.getOrElse(Seq()))
      paramTypeDataByParamTypes <- DBIO.sequence(paramTypes.map { paramType =>
        BehaviorParameterTypeData.fromAction(paramType, dataService).map { data =>
          (paramType, data)
        }
      }).map(_.toMap)
      maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.triggers.allForAction(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeDataTypeConfig <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.dataTypeConfigs.maybeForAction(behaviorVersion)
      }.getOrElse(DBIO.successful(None))
      maybeDataTypeConfigData <- maybeDataTypeConfig.map { config =>
        DataTypeConfigData.forConfigAction(config, dataService).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      for {
        behavior <- maybeBehavior
        behaviorVersion <- maybeBehaviorVersion
        params <- maybeParameters
        triggers <- maybeTriggers
      } yield {
        val maybeEnsuredDataTypeConfigData = maybeDataTypeConfigData.orElse(maybeDataTypeConfigFor(behaviorVersion.isDataType, behaviorVersion.maybeName))
        val config = BehaviorConfig(
          maybeExportId,
          behaviorVersion.maybeName,
          behaviorVersion.responseType.id,
          Some(behaviorVersion.canBeMemoized),
          isDataType = maybeEnsuredDataTypeConfigData.isDefined,
          isTest = Some(behaviorVersion.isTest),
          maybeEnsuredDataTypeConfigData
        )

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
          triggers.map(BehaviorTriggerData.fromTrigger),
          config,
          behavior.maybeExportId,
          Some(behaviorVersion.createdAt)
        )
      }
    }
  }

  def maybeFor(
                behaviorId: String,
                user: User,
                dataService: DataService,
                maybeGroupVersion: Option[BehaviorGroupVersion],
                maybeExportId: Option[String]
              )(implicit ec: ExecutionContext): Future[Option[BehaviorVersionData]] = {
    dataService.run(maybeForAction(behaviorId, user, dataService, maybeGroupVersion, maybeExportId))
  }
}
