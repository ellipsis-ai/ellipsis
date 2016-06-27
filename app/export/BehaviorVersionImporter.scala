package export

import json.EditorFormat.BehaviorVersionData
import models.Team
import models.bots.{BehaviorQueries, BehaviorVersionQueries, BehaviorVersion}
import services.AWSLambdaService
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorVersionImporter(team: Team, lambdaService: AWSLambdaService, data: BehaviorVersionData) {

  def run: DBIO[BehaviorVersion] = {
    for {
      behavior <- BehaviorQueries.createFor(team)
      version <- BehaviorVersionQueries.createFor(behavior, lambdaService, data)
    } yield version

  }

}
