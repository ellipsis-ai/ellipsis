package export

import java.io.{File, PrintWriter}

import json.Formatting._
import json.{BehaviorGroupConfig, BehaviorGroupData, BehaviorVersionData, LibraryVersionData}
import models.IDs
import models.accounts.user.User
import play.api.libs.json.Json
import services.DataService
import utils.SafeFileName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.io.Path
import scala.sys.process.Process

case class BehaviorGroupExporter(
                                  groupData: BehaviorGroupData,
                                  functionMap: Map[String, Option[String]],
                                  exportName: String,
                                  parentPath: String
                                ) {

  val groupPath = s"$parentPath/${exportName}"
  def zipFileName = s"$groupPath.zip"

  val config = BehaviorGroupConfig(
    groupData.name.getOrElse(""),
    groupData.exportId,
    groupData.icon,
    groupData.awsConfig,
    groupData.requiredOAuth2ApiConfigs,
    groupData.requiredSimpleTokenApis
  ).copyForExport

  val actionInputs = groupData.actionInputs

  def configString: String = Json.prettyPrint(Json.toJson(config))

  def writeDataTypeInputs(): Unit = {
    val forExport = groupData.dataTypeInputs.map(_.copyForExport(this)).sortBy(_.exportId)
    writeFileFor(groupPath, "data_type_inputs.json", Json.prettyPrint(Json.toJson(forExport)))
  }

  def createDirectoryFor(fullPath: String): Unit = {
    val path = Path(fullPath)
    path.deleteRecursively()
    path.createDirectory()
  }

  protected def writeFileFor(path: String, filename: String, content: String): Unit = {
    val writer = new PrintWriter(new File(s"$path/$filename"))
    writer.write(content)
    writer.close()
  }

  def writeDataTypes(): Unit = {
    writeDataTypeInputs()
    groupData.dataTypeBehaviorVersions.foreach { ea =>
      createDirectoryFor(fullPathFor(ea))
      writeFilesFor(ea)
    }
  }

  def writeActionInputs(): Unit = {
    val forExport = actionInputs.map(_.copyForExport(this)).sortBy(_.exportId)
    writeFileFor(groupPath, "action_inputs.json", Json.prettyPrint(Json.toJson(forExport)))
  }

  def writeLibraries(): Unit = {
    val fullPath = s"$groupPath/lib"
    if (groupData.libraryVersions.nonEmpty) {
      createDirectoryFor(fullPath)
    }
    groupData.libraryVersions.foreach { libraryVersion =>
      libraryVersion.maybeExportName.foreach { exportName =>
        val forExport = groupData.libraryVersions.map(_.copyForExport(this)).sortBy(_.exportId)
        writeFileFor(fullPath, exportName, libraryVersion.exportFileContent)
      }
    }
  }

  def fullPathFor(libraryVersionData: LibraryVersionData): String = {
    val safeDirName = SafeFileName.forName(libraryVersionData.maybeExportName.getOrElse(IDs.next))
    s"$groupPath/lib/$safeDirName"
  }

  def fullPathFor(behaviorVersionData: BehaviorVersionData): String = {
    val behaviorType = if (behaviorVersionData.isDataType) { "data_types" } else { "actions" }
    val safeDirName = SafeFileName.forName(behaviorVersionData.maybeExportName.getOrElse(IDs.next))
    s"$groupPath/$behaviorType/$safeDirName"
  }

  def functionStringFor(behaviorVersionData: BehaviorVersionData): String = {
    functionMap.get(behaviorVersionData.exportId.get).flatten.getOrElse("")
  }

  def paramsStringFor(behaviorVersionData: BehaviorVersionData): String = {
    val inputExportIds = behaviorVersionData.inputIds.map { ea =>
      groupData.inputs.find(_.inputId.contains(ea)).map(_.exportId).get
    }
    Json.prettyPrint(Json.toJson(inputExportIds))
  }

  def triggersStringFor(behaviorVersionData: BehaviorVersionData): String = {
    Json.prettyPrint(Json.toJson(behaviorVersionData.triggers))
  }

  def configStringFor(behaviorVersionData: BehaviorVersionData): String = {
    Json.prettyPrint(Json.toJson(behaviorVersionData.config.copyForExport(this)))
  }

  protected def writeFilesFor(behaviorVersionData: BehaviorVersionData): Unit = {
    val path = fullPathFor(behaviorVersionData)
    writeFileFor(path, "README", behaviorVersionData.description.getOrElse(""))
    writeFileFor(path, "function.js", functionStringFor(behaviorVersionData))
    writeFileFor(path, "triggers.json", triggersStringFor(behaviorVersionData))
    writeFileFor(path, "params.json", paramsStringFor(behaviorVersionData))
    writeFileFor(path, "response.md", behaviorVersionData.responseTemplate)
    writeFileFor(path, "config.json", configStringFor(behaviorVersionData))
  }

  def writeActions(): Unit = {
    writeActionInputs()
    groupData.actionBehaviorVersions.foreach { ea =>
      createDirectoryFor(fullPathFor(ea))
      writeFilesFor(ea)
    }
  }

  protected def writeFiles(): Unit = {
    writeFileFor(groupPath, "config.json", configString)
    groupData.description.foreach { desc =>
      writeFileFor(groupPath, "README", desc)
    }
    writeDataTypes()
    writeActions()
    writeLibraries()
  }

  def createDirectory(fullPath: String): Unit = {
    val path = Path(fullPath)
    path.deleteRecursively()
    path.createDirectory()
    writeFiles()
  }

  protected def createZip(): Unit = {
    createDirectory(groupPath)
    val path = Path(zipFileName)
    path.delete()
    Process(Seq("bash","-c",s"cd $groupPath && zip -r $zipFileName *")).!
  }

  def getZipFile: File = {
    createZip()
    new File(zipFileName)
  }

}

object BehaviorGroupExporter {

  def maybeFor(groupId: String, user: User, dataService: DataService): Future[Option[BehaviorGroupExporter]] = {
    val mainParentPath = "/tmp/exports/"
    for {
      maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
      maybeGroupVersion <- maybeGroup.map { group =>
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      }.getOrElse(Future.successful(None))
      maybeGroupData <- BehaviorGroupData.maybeFor(groupId, user, None, dataService)
      functionMap <- maybeGroupData.map { groupData =>
        Future.sequence(groupData.behaviorVersions.map { ea =>
          ea.maybeFunction(dataService).map { maybeFunction =>
            (ea.exportId.get, maybeFunction)
          }
        }).map(_.toMap[String, Option[String]])
      }.getOrElse(Future.successful(Map[String, Option[String]]()))
    } yield {
      for {
        groupVersion <- maybeGroupVersion
        groupData <- maybeGroupData
      } yield {
        BehaviorGroupExporter(groupData, functionMap, groupVersion.exportName, mainParentPath)
      }
    }
  }
}
