package export

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
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
    for {
      group <- dataService.behaviorGroups.createFor(data.name, data.icon, data.description, data.exportId, team)
      _ <- dataService.behaviorGroupVersions.createFor(group, user, data.copyForImportOf(group))
    } yield Some(group)

  }

}
