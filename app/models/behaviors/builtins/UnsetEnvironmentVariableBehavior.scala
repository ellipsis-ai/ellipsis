package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class UnsetEnvironmentVariableBehavior(
                                           name: String,
                                           messageContext: MessageContext,
                                           lambdaService: AWSLambdaService,
                                           dataService: DataService
                                           ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      didDelete <- maybeTeam.map { team =>
        dataService.teamEnvironmentVariables.deleteFor(name, team)
      }.getOrElse(Future.successful(false))
    } yield {
      val msg = if (didDelete) {
        s"OK, I deleted the env var `$name`"
      } else {
        s"I couldn't find `$name` to delete"
      }
      SimpleTextResult(msg, forcePrivateResponse = false)
    }
  }

}
