package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class SetEnvironmentVariableBehavior(
                                          name: String,
                                          value: String,
                                          messageContext: MessageContext,
                                          lambdaService: AWSLambdaService,
                                          dataService: DataService
                                           ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        dataService.environmentVariables.ensureFor(name, Some(value), team)
      }.getOrElse(Future.successful(None))
    } yield {
      SimpleTextResult(s"OK, saved $name!", forcePrivateResponse = false)
    }
  }

}
