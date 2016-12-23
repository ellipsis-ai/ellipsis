package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.NewMessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class SetEnvironmentVariableBehavior(
                                           name: String,
                                           value: String,
                                           event: NewMessageEvent,
                                           lambdaService: AWSLambdaService,
                                           dataService: DataService
                                           ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        dataService.teamEnvironmentVariables.ensureFor(name, Some(value), team)
      }.getOrElse(Future.successful(None))
    } yield {
      SimpleTextResult(s"OK, saved $name!", forcePrivateResponse = false)
    }
  }

}
