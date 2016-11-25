package export

import json.BehaviorVersionData
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorVersionImporter(
                                    team: Team,
                                    user: User,
                                    data: BehaviorVersionData,
                                    dataService: DataService
                                  ) extends Importer[Option[BehaviorVersion]] {

  def run: Future[Option[BehaviorVersion]] = {
    for {
      version <- for {
        dataTypeVersionData <- Future.successful {
          data.params.flatMap(_.paramType.flatMap(_.behavior))
        }
        prereqs <- Future.sequence(dataTypeVersionData.map { versionData =>
          BehaviorVersionImporter(team, user, versionData, dataService).run
        }).map(_.flatten)
        maybeGroup <- data.groupId.map { gid =>
          dataService.behaviorGroups.find(gid)
        }.getOrElse(Future.successful(None))
        behavior <- maybeGroup.map { group =>
          dataService.behaviors.createFor(group, data.config.publishedId, data.config.dataTypeName)
        }.getOrElse {
          dataService.behaviors.createFor(team, data.config.publishedId, data.config.dataTypeName)
        }
        version <- dataService.behaviorVersions.createFor(behavior, Some(user), data)
      } yield Some(version)
    } yield version
  }

}
