package export

import json.BehaviorVersionData
import models.Team
import models.bots.{BehaviorQueries, BehaviorVersionQueries, BehaviorVersion}
import services.AWSLambdaService
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorVersionImporter(team: Team, lambdaService: AWSLambdaService, data: BehaviorVersionData) {

  def run: DBIO[BehaviorVersion] = {
    for {
      behavior <- BehaviorQueries.createFor(team, data.config.map(_.publishedId))
      version <- BehaviorVersionQueries.createFor(behavior, lambdaService, data)
    } yield version

  }

}
