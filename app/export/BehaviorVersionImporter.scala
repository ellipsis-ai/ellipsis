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
                                  ) {

  def run: Future[BehaviorVersion] = {
    for {
      behavior <- dataService.behaviors.createFor(team, data.config.publishedId)
      version <- dataService.behaviorVersions.createFor(behavior, Some(user), data)
    } yield version

  }

}
