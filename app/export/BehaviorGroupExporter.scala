package export

import java.io.File

import json.{BehaviorGroupConfig, InputData}
import json.Formatting._
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.Json
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.io.Path
import scala.sys.process.Process

case class BehaviorGroupExporter(
                                  behaviorGroup: BehaviorGroup,
                                  inputsData: Seq[InputData],
                                  behaviorExporters: Seq[BehaviorVersionExporter],
                                  parentPath: String
                                ) extends Exporter {

  val fullPath = s"$parentPath/${behaviorGroup.exportName}"
  def zipFileName = s"$fullPath.zip"

  val config = {
    val publishedId = behaviorGroup.maybeImportedId.getOrElse(behaviorGroup.id)
    BehaviorGroupConfig(behaviorGroup.name, publishedId, behaviorGroup.maybeIcon)
  }

  def configString: String = Json.prettyPrint(Json.toJson(config))

  def dataTypeExporters: Seq[BehaviorVersionExporter] = behaviorExporters.filter(_.behaviorVersion.behavior.isDataType)

  def writeDataTypes(): Unit = {
    dataTypeExporters.foreach { ea =>
      ea.createDirectory()
    }
  }

  def exportIdForInputId(inputId: String): Option[String] = {
    inputsData.find(_.id == inputId).flatMap(_.exportId)
  }

  def exportIdForDataTypeId(dataTypeId: String): Option[String] = {
    dataTypeExporters.find(_.behaviorVersion.behavior.id == dataTypeId).flatMap(_.config.publishedId)
  }

  def writeInputs(): Unit = {
    val forExport = inputsData.map(_.copyForExport(this)).sortBy(_.exportId)
    writeFileFor("inputs.json", Json.prettyPrint(Json.toJson(forExport)))
  }

  def writeActions(): Unit = {
    behaviorExporters.filterNot(_.behaviorVersion.behavior.isDataType).foreach { ea =>
      ea.copyForExport(this).createDirectory()
    }
  }

  protected def writeFiles(): Unit = {
    writeFileFor("config.json", configString)
    behaviorGroup.maybeDescription.foreach { desc =>
      writeFileFor("README", desc)
    }
    writeDataTypes()
    writeInputs()
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
      maybeGroup <- dataService.behaviorGroups.find(groupId)
      maybeBehaviors <- maybeGroup.map { group =>
        dataService.behaviors.allForGroup(group).map(Some(_))
      }.getOrElse(Future.successful(None))
      _ <- maybeBehaviors.map { behaviors =>
        Future.sequence(behaviors.map { behavior =>
          dataService.inputs.ensureExportIdsFor(behavior)
        }).map(_ => {})
      }.getOrElse(Future.successful({}))
      maybeExporters <- maybeBehaviors.map { behaviors =>
        val exportName = maybeGroup.map(_.exportName).get
        val parentPath = s"$mainParentPath/$exportName"
        Future.sequence(behaviors.map { behavior =>
          BehaviorVersionExporter.maybeFor(behavior.id, user, parentPath, dataService)
        }).map(e => Some(e.flatten))
      }.getOrElse(Future.successful(None))
      inputs <- maybeExporters.map { exporters =>
        Future.sequence(exporters.map { ea =>
          val inputIds = ea.paramsData.flatMap(_.inputId)
          Future.sequence(inputIds.map { id =>
            dataService.inputs.find(id)
          }).map(_.flatten)
        }).map(_.flatten.distinct)
      }.getOrElse(Future.successful(Seq()))
      inputsData <- Future.sequence(inputs.map { ea =>
        InputData.fromInput(ea, dataService)
      })
    } yield {
      for {
        group <- maybeGroup
        exporters <- maybeExporters
      } yield BehaviorGroupExporter(group, inputsData, exporters, mainParentPath)
    }
  }
}
