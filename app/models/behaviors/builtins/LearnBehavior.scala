package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

case class LearnBehavior(
                          event: Event,
                          lambdaService: AWSLambdaService,
                          dataService: DataService
                        ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    Future.successful(SimpleTextResult(event, s"I love to learn. Come ${event.teachMeLinkFor(lambdaService)}.", forcePrivateResponse = false))
  }

}
