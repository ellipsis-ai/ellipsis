package models.bots

import models.Team
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

object DefaultBehaviors {

  private def createEchoFor(team: Team): DBIO[Unit] = {
    for {
      behavior <- BehaviorQueries.createFor(team, "Echo")
      trigger <- RegexMessageTriggerQueries.ensureFor(behavior, s"""<@\\w+>:\\s+echo\\s+(.+)""".r)
    } yield Unit
  }

  def ensureFor(team: Team): DBIO[Unit] = {
    BehaviorQueries.allForTeam(team).flatMap { existing =>
      if (existing.isEmpty) {
        createEchoFor(team)
      } else {
        DBIO.successful(Unit)
      }
    }
  }

  def ensureForAll: DBIO[Unit] = {
    Team.all.result.flatMap { teams =>
      DBIO.sequence(teams.map(ensureFor)).map(_ => Unit)
    }
  }

}
