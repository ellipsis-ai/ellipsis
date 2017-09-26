package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class LearnBehavior(
                          event: Event,
                          services: DefaultServices
                        ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val lambdaService = services.lambdaService
    Future.successful(SimpleTextResult(event, None, s"I love to learn. Come ${event.teachMeLinkFor(lambdaService)}.", forcePrivateResponse = false))
  }

}
