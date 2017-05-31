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
      group <- dataService.behaviorGroups.createFor(data.exportId, team)
      oauth2Applications <- dataService.oauth2Applications.allUsableFor(team)
      _ <- dataService.behaviorGroupVersions.createFor(group, user, data.copyForNewVersionOf(group).copyWithApiApplicationsIfAvailable(oauth2Applications))
    } yield Some(group)

  }

}
