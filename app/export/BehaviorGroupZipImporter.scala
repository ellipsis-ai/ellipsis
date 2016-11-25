package export

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.util.zip.{ZipEntry, ZipInputStream}

import json.{BehaviorGroupConfig, BehaviorVersionData}
import json.Formatting._
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
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

    val versionFileRegex = """(actions|data_types)/([^/]+)/(.+)""".r
    val readmeRegex = """^README$""".r
    val configRegex = """^config.json$""".r

    var groupName: String = ""
    var maybePublishedId: Option[String] = None

    while (nextEntry != null) {
      val entryName = nextEntry.getName
      versionFileRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val versionId = firstMatch.subgroups(1)
        val filename = firstMatch.subgroups(2)
        val map = versionStringMaps.get(versionId).getOrElse {
          val newMap = scala.collection.mutable.Map[String, String]()
          versionStringMaps.put(versionId, newMap)
          newMap
        }
        map.put(filename, readDataFrom(zipInputStream))
      }
      readmeRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        groupName = readDataFrom(zipInputStream)
      }
      configRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val data = readDataFrom(zipInputStream)
        Json.parse(data).validate[BehaviorGroupConfig] match {
          case JsSuccess(data, jsPath) => {
            maybePublishedId = Some(data.publishedId)
          }
          case e: JsError =>
        }
      }
      nextEntry = zipInputStream.getNextEntry
    }

    dataService.behaviorGroups.createFor(groupName, maybePublishedId, team).flatMap { group =>
      val importers = versionStringMaps.map { case(versionId, strings) =>
        val data = BehaviorVersionData.fromStrings(
          team.id,
          strings.get("README"),
          strings.getOrElse("function.js", ""),
          strings.getOrElse("response.md", ""),
          strings.getOrElse("params.json", ""),
          strings.getOrElse("triggers.json", ""),
          strings.getOrElse("config.json", ""),
          maybeGithubUrl = None,
          dataService
        ).copy(groupId = Some(group.id))
        BehaviorVersionImporter(team, user, data, dataService)
      }

      Future.sequence(
        importers.map { importer =>
          importer.run
        }
      ).map(_.flatten).map { behaviorVersions =>
        Some(group)
      }
    }

  }

}
