package export

import java.io.File

import json.BehaviorVersionData
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService

import scala.collection.mutable

case class BehaviorVersionZipImporter(
                                       team: Team,
                                       user: User,
                                       zipFile: File,
                                       dataService: DataService
                                     ) extends ZipImporter[BehaviorVersion] {

  def importerFrom(strings: mutable.Map[String, String]): Importer[BehaviorVersion] = {
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

    BehaviorVersionImporter(team, user, data, dataService)
  }

}
