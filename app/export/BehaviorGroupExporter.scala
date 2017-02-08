package export

import java.io.File

import json.BehaviorGroupConfig
import json.Formatting._
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.input.Input
import play.api.libs.json.Json
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.io.Path
import scala.sys.process.Process

case class BehaviorGroupExporter(
                                  behaviorGroup: BehaviorGroup,
                                  sharedInputs: Seq[Input],
                                  behaviorExporters: Seq[BehaviorVersionExporter],
                                  parentPath: String
                                ) extends Exporter {

  val inputIdMapping = collection.mutable.Map[String, String]()
  val dataTypeIdMapping = collection.mutable.Map[String, String]()

  val fullPath = s"$parentPath/${behaviorGroup.exportName}"
  def zipFileName = s"$fullPath.zip"

  val config = BehaviorGroupConfig(behaviorGroup.name, behaviorGroup.id, None)

  def configString: String = Json.prettyPrint(Json.toJson(config))

  def writeDataTypes(): Unit = {
    behaviorExporters.filter(_.behaviorVersion.behavior.isDataType).foreach { ea =>
      dataTypeIdMapping.put(ea.behaviorVersion.id, ea.exportId)
      ea.createDirectory()
    }
  }

  def writeInputs(): Unit = {
    val inputs = behaviorExporters.flatMap(_.paramsData.map(_.inputData))
    val inputData = inputs.flatMap { input =>
      val maybeExisting = inputIdMapping.get(input.id.get)
      if (maybeExisting.isDefined) {
        None
      } else {
        val exportId = IDs.next
        inputIdMapping.put(input.id.get, exportId)
        Some(input.copy(id = Some(exportId)))
      }
    }
    writeFileFor("inputs.json", Json.prettyPrint(Json.toJson(inputData)))
  }

  def writeActions(): Unit = {
    behaviorExporters.filterNot(_.behaviorVersion.behavior.isDataType).foreach { ea =>
      ea.copyWithIdMappings(dataTypeIdMapping.toMap, inputIdMapping.toMap).createDirectory()
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
      sharedInputs <- maybeGroup.map { group =>
        dataService.inputs.allForGroup(group)
      }.getOrElse(Future.successful(Seq()))
      maybeExporters <- maybeBehaviors.map { behaviors =>
        val exportName = maybeGroup.map(_.exportName).get
        val exportId = IDs.next
        val parentPath = s"$mainParentPath/$exportName"
        Future.sequence(behaviors.map { behavior =>
          BehaviorVersionExporter.maybeFor(exportId, behavior.id, user, parentPath, dataService)
        }).map(e => Some(e.flatten))
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        group <- maybeGroup
        exporters <- maybeExporters
      } yield BehaviorGroupExporter(group, sharedInputs, exporters, mainParentPath)
    }
  }
}
