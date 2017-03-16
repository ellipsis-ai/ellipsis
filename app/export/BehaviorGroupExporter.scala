package export

import java.io.File

import json.{BehaviorGroupConfig, InputData}
import json.Formatting._
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import play.api.libs.json.Json
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.io.Path
import scala.sys.process.Process

case class BehaviorGroupExporter(
                                  behaviorGroupVersion: BehaviorGroupVersion,
                                  actionInputsData: Seq[InputData],
                                  actionExporters: Seq[BehaviorVersionExporter],
                                  dataTypeInputsData: Seq[InputData],
                                  dataTypeExporters: Seq[BehaviorVersionExporter],
                                  parentPath: String
                                ) extends Exporter {

  val fullPath = s"$parentPath/${behaviorGroupVersion.exportName}"
  def zipFileName = s"$fullPath.zip"

  val config = {
    BehaviorGroupConfig(behaviorGroupVersion.name, behaviorGroupVersion.group.maybeExportId, behaviorGroupVersion.maybeIcon)
  }

  def configString: String = Json.prettyPrint(Json.toJson(config))

  def writeDataTypeInputs(): Unit = {
    val forExport = dataTypeInputsData.map(_.copyForExport(this)).sortBy(_.exportId)
    writeFileFor("data_type_inputs.json", Json.prettyPrint(Json.toJson(forExport)))
  }

  def writeDataTypes(): Unit = {
    writeDataTypeInputs()
    dataTypeExporters.foreach { ea =>
      ea.createDirectory()
    }
  }

  def exportIdForInputId(inputId: String): Option[String] = {
    actionInputsData.find(_.id == inputId).flatMap(_.exportId)
  }

  def exportIdForDataTypeId(dataTypeId: String): Option[String] = {
    dataTypeExporters.find(_.behaviorVersion.behavior.id == dataTypeId).flatMap(_.config.exportId)
  }

  def writeActionInputs(): Unit = {
    val forExport = actionInputsData.map(_.copyForExport(this)).sortBy(_.exportId)
    writeFileFor("action_inputs.json", Json.prettyPrint(Json.toJson(forExport)))
  }

  def writeActions(): Unit = {
    writeActionInputs()
    actionExporters.foreach { ea =>
      ea.createDirectory()
    }
  }

  protected def writeFiles(): Unit = {
    writeFileFor("config.json", configString)
    behaviorGroupVersion.maybeDescription.foreach { desc =>
      writeFileFor("README", desc)
    }
    writeDataTypes()
    writeActions()
  }

  protected def createZip(): Unit = {
    createDirectory()
    val path = Path(zipFileName)
    path.delete()
    Process(Seq("bash","-c",s"cd $fullPath && zip -r $zipFileName *")).!
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
      maybeGroup <- dataService.behaviorGroups.find(groupId).flatMap { maybeGroup =>
        maybeGroup.map { group =>
          dataService.behaviorGroups.ensureExportIdFor(group).map(Some(_))
        }.getOrElse(Future.successful(None))
      }
      maybeCurrentGroupVersion <- maybeGroup.map { group =>
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      }.getOrElse(Future.successful(None))
      inputs <- maybeCurrentGroupVersion.map { groupVersion =>
        dataService.inputs.allForGroupVersion(groupVersion)
      }.getOrElse(Future.successful(Seq()))
      inputsData <- Future.sequence(inputs.map { input =>
        InputData.fromInput(input, dataService)
      })
      maybeBehaviors <- maybeGroup.map { group =>
        dataService.behaviors.allForGroup(group).map(Some(_))
      }.getOrElse(Future.successful(None))
      _ <- maybeBehaviors.map { behaviors =>
        Future.sequence(behaviors.map { behavior =>
          dataService.inputs.ensureExportIdsFor(behavior)
        }).map(_ => {})
      }.getOrElse(Future.successful({}))
      maybeExporters <- maybeBehaviors.map { behaviors =>
        val exportName = maybeCurrentGroupVersion.map(_.exportName).get
        val parentPath = s"$mainParentPath/$exportName"
        Future.sequence(behaviors.map { behavior =>
          BehaviorVersionExporter.maybeFor(behavior.id, user, parentPath, dataService)
        }).map(e => Some(e.flatten))
      }.getOrElse(Future.successful(None))
      (dataTypeExporters, actionExporters) <- Future.successful(maybeExporters.map { exporters =>
        exporters.partition(_.behaviorVersion.behavior.isDataType)
      }.getOrElse((Seq(), Seq())))
    } yield {
      val (dataTypeInputsData, actionInputsData) = inputsData.partition { ea =>
        dataTypeExporters.exists(exporter => ea.inputId.exists(exporter.inputIds.contains))
      }
      maybeCurrentGroupVersion.map { groupVersion =>
        BehaviorGroupExporter(groupVersion, actionInputsData, actionExporters, dataTypeInputsData, dataTypeExporters, mainParentPath)
      }
    }
  }
}
