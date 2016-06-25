package export

import java.io.{ByteArrayOutputStream, FileInputStream, File}
import java.util.zip.{ZipEntry, ZipInputStream}

import json.EditorFormat.{BehaviorVersionData, BehaviorTriggerData, BehaviorParameterData}
import models.Team
import models.bots.{BehaviorQueries, BehaviorVersionQueries, BehaviorVersion}
import play.api.libs.json.Json
import services.AWSLambdaService
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorVersionImporter(team: Team, lambdaService: AWSLambdaService, zipFile: File) {

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

  private def extractFunctionBodyFrom(function: String): String = {
    """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
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
      BehaviorVersionData(
        team.id,
        None,
        extractFunctionBodyFrom(strings.getOrElse("function.js", "")),
        strings.getOrElse("response.md", ""),
        Json.parse(strings.getOrElse("params.json", "")).validate[Seq[BehaviorParameterData]].get,
        Json.parse(strings.getOrElse("triggers.json", "")).validate[Seq[BehaviorTriggerData]].get,
        None
      )

    for {
      behavior <- BehaviorQueries.createFor(team)
      version <- BehaviorVersionQueries.createFor(behavior, lambdaService, data)
    } yield version

  }

}
