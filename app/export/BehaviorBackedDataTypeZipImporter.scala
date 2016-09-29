package export

import java.io.File

import json.BehaviorBackedDataTypeData
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorBackedDataType
import services.DataService

case class BehaviorBackedDataTypeZipImporter(
                                             team: Team,
                                             user: User,
                                             zipFile: File,
                                             dataService: DataService
                                           ) extends ZipImporter[BehaviorBackedDataType] {

  def importerFrom(strings: Map[String, String]): Importer[BehaviorBackedDataType] = {
    val data =
      BehaviorBackedDataTypeData.fromStrings(
        team.id,
        strings.getOrElse("function.js", ""),
        strings.getOrElse("config.json", ""),
        maybeGithubUrl = None,
        dataService
      )

    BehaviorBackedDataTypeImporter(team, user, data, dataService)
  }

}
