package models.bots.builtins

import models.{EnvironmentVariableQueries, Team}
import models.bots.MessageContext
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global


case class SetEnvironmentVariableBehavior(
                                          name: String,
                                          value: String,
                                          messageContext: MessageContext,
                                          lambdaService: AWSLambdaService
                                           ) extends BuiltinBehavior {

  def run: DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        EnvironmentVariableQueries.ensureFor(name, Some(value), team)
      }.getOrElse(DBIO.successful(None))
    } yield {
      messageContext.sendMessage(s"OK, saved $name!")
    }
  }

}
