package models.bots.builtins

import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import models.{EnvironmentVariableQueries, Team}
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class SetEnvironmentVariableBehavior(
                                          name: String,
                                          value: String,
                                          messageContext: MessageContext,
                                          lambdaService: AWSLambdaService
                                           ) extends BuiltinBehavior {

  def result: DBIO[BehaviorResult] = {
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        EnvironmentVariableQueries.ensureFor(name, Some(value), team)
      }.getOrElse(DBIO.successful(None))
    } yield {
      SimpleTextResult(s"OK, saved $name!")
    }
  }

}
