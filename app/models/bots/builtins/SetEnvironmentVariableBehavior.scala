package models.bots.builtins

import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
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

  def result: Future[BehaviorResult] = {
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        dataService.environmentVariables.ensureFor(name, Some(value), team)
      }.getOrElse(Future.successful(None))
    } yield {
      SimpleTextResult(s"OK, saved $name!")
    }
  }

}
