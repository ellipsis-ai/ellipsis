package export

import java.io.File

import json.BehaviorGroupConfig
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
                                  behaviorExporters: Seq[BehaviorVersionExporter],
                                  parentPath: String
                                ) extends Exporter {

  val dirName = s"$parentPath/${behaviorGroup.id}"
  def zipFileName = s"$dirName.zip"

  val config = BehaviorGroupConfig(behaviorGroup.id)

  def configString: String = Json.prettyPrint(Json.toJson(config))

  protected def writeFiles(): Unit = {
    writeFileFor("config.json", configString)
    behaviorGroup.maybeDescription.foreach { desc =>
      writeFileFor("README", desc)
    }
    behaviorExporters.foreach { exporter =>
      exporter.createDirectory()
    }
  }

  protected def createZip(): Unit = {
    createDirectory()
    val path = Path(zipFileName)
    path.delete()
    Process(Seq("bash","-c",s"cd $dirName && zip -r $zipFileName *")).!
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
      maybeExporters <- maybeBehaviors.map { behaviors =>
        val groupId = maybeGroup.map(_.id).get
        val parentPath = s"$mainParentPath/$groupId"
        Future.sequence(behaviors.map { behavior =>
          BehaviorVersionExporter.maybeFor(behavior.id, user, parentPath, dataService)
        }).map(e => Some(e.flatten))
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        group <- maybeGroup
        exporters <- maybeExporters
      } yield BehaviorGroupExporter(group, exporters, mainParentPath)
    }
  }
}
