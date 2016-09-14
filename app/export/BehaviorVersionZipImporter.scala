package export

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.util.zip.{ZipEntry, ZipInputStream}

import json.BehaviorVersionData
import models.team.Team
import models.accounts.user.User
import models.bots.behaviorversion.BehaviorVersion
import services.{AWSLambdaService, DataService}
import slick.dbio.DBIO

case class BehaviorVersionZipImporter(
                                       team: Team,
                                       user: User,
                                       lambdaService: AWSLambdaService,
                                       zipFile: File,
                                       dataService: DataService
                                     ) {

  private def readDataFrom(zipInputStream: ZipInputStream): String = {
    val buffer = new Array[Byte](1024)
    val out = new ByteArrayOutputStream()
    var len: Int = zipInputStream.read(buffer)

    while (len > 0) {
      out.write(buffer, 0, len)
      len = zipInputStream.read(buffer)
    }

    out.toString
  }

  def run: DBIO[BehaviorVersion] = {

    val zipInputStream: ZipInputStream = new ZipInputStream(new FileInputStream(zipFile))
    var entry: ZipEntry = zipInputStream.getNextEntry
    val strings = scala.collection.mutable.Map[String, String]()

    while (entry != null) {
      strings.put(entry.getName, readDataFrom(zipInputStream))
      entry = zipInputStream.getNextEntry
    }

    val data =
      BehaviorVersionData.fromStrings(
        team.id,
        strings.getOrElse("function.js", ""),
        strings.getOrElse("response.md", ""),
        strings.getOrElse("params.json", ""),
        strings.getOrElse("triggers.json", ""),
        strings.getOrElse("config.json", ""),
        maybeGithubUrl = None,
        dataService
      )

    DBIO.from(BehaviorVersionImporter(team, user, data, dataService).run)
  }

}
