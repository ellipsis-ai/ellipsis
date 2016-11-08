package export

import java.io.File

import json.BehaviorVersionData
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService

case class BehaviorVersionZipImporter(
                                       team: Team,
                                       user: User,
                                       zipFile: File,
                                       dataService: DataService
                                     ) extends ZipImporter[Option[BehaviorVersion]] {

  def importerFrom(strings: Map[String, String]): Importer[Option[BehaviorVersion]] = {
    val data =
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

    BehaviorVersionImporter(team, user, data, dataService)
  }

}
