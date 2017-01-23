package models.behaviors.builtins

import models.behaviors.events.MessageEvent
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class UnsetEnvironmentVariableBehavior(
                                             name: String,
                                             event: MessageEvent,
                                             lambdaService: AWSLambdaService,
                                             dataService: DataService
                                           ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      didDelete <- maybeTeam.map { team =>
        dataService.teamEnvironmentVariables.deleteFor(name, team)
      }.getOrElse(Future.successful(false))
    } yield {
      val msg = if (didDelete) {
        s"OK, I deleted the env var `$name`"
      } else {
        s"I couldn't find `$name` to delete"
      }
      SimpleTextResult(event, msg, forcePrivateResponse = false)
    }
  }

}
