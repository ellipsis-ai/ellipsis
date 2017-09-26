package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class FeedbackBehavior(event: Event, services: DefaultServices) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val msg = "Something to say? Say it to my face."
    Future.successful(SimpleTextResult(event, None, msg, forcePrivateResponse = true))
  }

}
