package models.bots.builtins

import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import models.{EnvironmentVariableQueries, Team}
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class UnsetEnvironmentVariableBehavior(
                                           name: String,
                                           messageContext: MessageContext,
                                           lambdaService: AWSLambdaService
                                           ) extends BuiltinBehavior {

  def result: DBIO[BehaviorResult] = {
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      didDelete <- maybeTeam.map { team =>
        EnvironmentVariableQueries.deleteFor(name, team)
      }.getOrElse(DBIO.successful(false))
    } yield {
      val msg = if (didDelete) {
        s"OK, I deleted the env var `$name`"
      } else {
        s"I couldn't find `$name` to delete"
      }
      SimpleTextResult(msg)
    }
  }

}
