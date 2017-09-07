package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}


case class SetEnvironmentVariableBehavior(
                                           name: String,
                                           value: String,
                                           event: Event,
                                           lambdaService: AWSLambdaService,
                                           dataService: DataService
                                           ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        dataService.teamEnvironmentVariables.ensureFor(name, Some(value), team)
      }.getOrElse(Future.successful(None))
    } yield {
      SimpleTextResult(event, None, s"OK, saved $name!", forcePrivateResponse = false)
    }
  }

}
