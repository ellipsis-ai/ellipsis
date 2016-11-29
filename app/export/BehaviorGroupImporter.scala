package export

import json.BehaviorGroupData
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorGroupImporter(
                                   team: Team,
                                   user: User,
                                   data: BehaviorGroupData,
                                   dataService: DataService
                                 ) {

  def run: Future[Option[BehaviorGroup]] = {

    dataService.behaviorGroups.createFor(data.name, data.description, None, team).flatMap { group =>
      val behaviorVersionsWithGroup = data.behaviorVersions.map { ea =>
        ea.copy(groupId = Some(group.id))
      }
      val importers = behaviorVersionsWithGroup.map { versionData =>
        BehaviorVersionImporter(team, user, versionData, dataService)
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
