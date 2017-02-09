package export

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.time.OffsetDateTime
import java.util.zip.{ZipEntry, ZipInputStream}

import json.{BehaviorGroupConfig, BehaviorGroupData, BehaviorVersionData, InputData}
import json.Formatting._
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.DataService

import scala.concurrent.Future

case class BehaviorGroupZipImporter(
                                     team: Team,
                                     user: User,
                                     zipFile: File,
                                     dataService: DataService
                                   ) {

  protected def readDataFrom(zipInputStream: ZipInputStream): String = {
    val buffer = new Array[Byte](1024)
    val out = new ByteArrayOutputStream()
    var len: Int = zipInputStream.read(buffer)

    while (len > 0) {
      out.write(buffer, 0, len)
      len = zipInputStream.read(buffer)
    }

    out.toString
  }

  def run: Future[Option[BehaviorGroup]] = {

    val zipInputStream: ZipInputStream = new ZipInputStream(new FileInputStream(zipFile))
    var nextEntry: ZipEntry = zipInputStream.getNextEntry

    val versionStringMaps = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, String]]()

    val versionFileRegex = """^(actions|data_types)/([^/]+)/(.+)""".r
    val readmeRegex = """^README$$""".r
    val configRegex = """^config\.json$$""".r
    val inputsRegex = """^inputs\.json$$""".r

    var groupName: String = ""
    var groupDescription: String = ""
    var maybePublishedId: Option[String] = None
    var inputs: Seq[InputData] = Seq()

    while (nextEntry != null) {
      val entryName = nextEntry.getName
      versionFileRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val versionId = firstMatch.subgroups(1)
        val filename = firstMatch.subgroups(2)
        val map = versionStringMaps.getOrElse(versionId, {
          val newMap = scala.collection.mutable.Map[String, String]()
          versionStringMaps.put(versionId, newMap)
          newMap
        })
        map.put(filename, readDataFrom(zipInputStream))
      }
      readmeRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        groupDescription = readDataFrom(zipInputStream)
      }
      configRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val readData = readDataFrom(zipInputStream)
        Json.parse(readData).validate[BehaviorGroupConfig] match {
          case JsSuccess(data, jsPath) => {
            groupName = data.name
            maybePublishedId = Some(data.publishedId)
          }
          case e: JsError =>
        }
      }
      inputsRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val readData = readDataFrom(zipInputStream)
        Json.parse(readData).validate[Seq[InputData]] match {
          case JsSuccess(data, jsPath) => {
            inputs = data
          }
          case e: JsError =>
        }
      }
      nextEntry = zipInputStream.getNextEntry
    }

    val versionsData = versionStringMaps.map { case(versionId, strings) =>
      BehaviorVersionData.fromStrings(
        team.id,
        strings.get("README"),
        strings.getOrElse("function.js", ""),
        strings.getOrElse("response.md", ""),
        strings.getOrElse("params.json", ""),
        strings.getOrElse("triggers.json", ""),
        strings.getOrElse("config.json", ""),
        maybeGithubUrl = None,
        dataService
      )
    }.toSeq

    val data = BehaviorGroupData(
      None,
      groupName,
      groupDescription,
      icon = None,
      inputs,
      versionsData,
      githubUrl = None,
      importedId = maybePublishedId,
      maybePublishedId,
      OffsetDateTime.now
    )

    BehaviorGroupImporter(team, user, data, dataService).run

  }

}
