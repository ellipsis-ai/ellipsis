package export

import json.BehaviorVersionData
import models.Team
import models.accounts.user.User
import models.bots.{BehaviorQueries, BehaviorVersion, BehaviorVersionQueries}
import services.AWSLambdaService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorVersionImporter(team: Team, user: User, lambdaService: AWSLambdaService, data: BehaviorVersionData) {

  def run: DBIO[BehaviorVersion] = {
    for {
      behavior <- BehaviorQueries.createFor(team, data.config.publishedId)
      version <- BehaviorVersionQueries.createFor(behavior, Some(user), lambdaService, data)
    } yield version

  }

}
