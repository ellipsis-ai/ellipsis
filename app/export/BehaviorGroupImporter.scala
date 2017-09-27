package export

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorGroupImporter(
                                   team: Team,
                                   user: User,
                                   data: BehaviorGroupData,
                                   dataService: DataService
                                 ) {

  def run(implicit ec: ExecutionContext): Future[Option[BehaviorGroup]] = {
    for {
      maybeExistingGroup <- data.id.map { groupId =>
        dataService.behaviorGroups.findWithoutAccessCheck(groupId)
      }.getOrElse(Future.successful(None))
      group <- maybeExistingGroup.map(Future.successful).getOrElse {
        dataService.behaviorGroups.createFor(data.exportId, team)
      }
      oauth2Applications <- dataService.oauth2Applications.allUsableFor(team)
      _ <- dataService.behaviorGroupVersions.createFor(
        group,
        user,
        data.copyForNewVersionOf(group).copyWithApiApplicationsIfAvailable(oauth2Applications)
      )
    } yield Some(group)

  }

}
