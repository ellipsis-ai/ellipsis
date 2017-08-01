package export

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.time.OffsetDateTime
import java.util.zip.{ZipEntry, ZipInputStream}

import json.Formatting._
import json._
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
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

    val versionFileRegex = """^(actions|data_types)/([^/]+)/(.+)""".r
    val readmeRegex = """^README$$""".r
    val configRegex = """^config\.json$$""".r
    val actionInputsRegex = """^action_inputs\.json$$""".r
    val dataTypeInputsRegex = """^data_type_inputs\.json$$""".r

    var maybeGroupName: Option[String] = None
    var maybeGroupDescription: Option[String] = None
    var maybeExportId: Option[String] = None
    var maybeIcon: Option[String] = None
    var requiredOAuth2ApiConfigData: Seq[RequiredOAuth2ApiConfigData] = Seq()
    var requiredSimpleTokenApiData: Seq[RequiredSimpleTokenApiData] = Seq()
    var actionInputs: Seq[InputData] = Seq()
    var dataTypeInputs: Seq[InputData] = Seq()
    var libraries: Seq[LibraryVersionData] = Seq()

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
      LibraryVersionData.libFileRegex.findFirstMatchIn(entryName).foreach { _ =>
        val newLib = LibraryVersionData.fromFile(readDataFrom(zipInputStream), entryName)
        libraries ++= Seq(newLib)
      }
      readmeRegex.findFirstMatchIn(entryName).foreach { _ =>
        maybeGroupDescription = Some(readDataFrom(zipInputStream))
      }
      configRegex.findFirstMatchIn(entryName).foreach { _ =>
        val readData = readDataFrom(zipInputStream)
        Json.parse(readData).validate[BehaviorGroupConfig] match {
          case JsSuccess(data, _) => {
            maybeGroupName = Some(data.name)
            maybeExportId = data.exportId
            maybeIcon = data.icon
            requiredOAuth2ApiConfigData = data.requiredOAuth2ApiConfigs
            requiredSimpleTokenApiData = data.requiredSimpleTokenApis
          }
          case e: JsError =>
        }
      }
      actionInputsRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val readData = readDataFrom(zipInputStream)
        Json.parse(readData).validate[Seq[InputData]] match {
          case JsSuccess(data, jsPath) => {
            actionInputs = data
          }
          case e: JsError =>
        }
      }
      dataTypeInputsRegex.findFirstMatchIn(entryName).foreach { firstMatch =>
        val readData = readDataFrom(zipInputStream)
        Json.parse(readData).validate[Seq[InputData]] match {
          case JsSuccess(data, jsPath) => {
            dataTypeInputs = data
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
        strings.get("data_type_config.json"),
        maybeGithubUrl = None,
        dataService
      )
    }.toSeq

    for {
      alreadyInstalled <- dataService.behaviorGroups.allFor(team)
      alreadyInstalledData <- Future.sequence(alreadyInstalled.map { group =>
        BehaviorGroupData.maybeFor(group.id, user, None, dataService)
      }).map(_.flatten)
      maybeExistingGroupData <- Future.successful(alreadyInstalledData.find(_.exportId == maybeExportId))
      data <- Future.successful(
        BehaviorGroupData(
          None,
          team.id,
          maybeGroupName,
          maybeGroupDescription,
          maybeIcon,
          actionInputs,
          dataTypeInputs,
          versionsData,
          libraries,
          requiredOAuth2ApiConfigData,
          requiredSimpleTokenApiData,
          githubUrl = None,
          exportId = maybeExportId,
          Some(OffsetDateTime.now)
        ).copyForImportableForTeam(team, maybeExistingGroupData)
      )
      maybeImported <- BehaviorGroupImporter(team, user, data, dataService).run
    } yield maybeImported
  }

}
