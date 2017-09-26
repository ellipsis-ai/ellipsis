package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.Event
import services.{AWSLambdaService, DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class FeedbackBehavior(event: Event, services: DefaultServices) extends BuiltinBehavior {
  val dataService: DataService = services.dataService
  val lambdaService: AWSLambdaService = services.lambdaService

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val msg = "Something to say? Say it to my face."
    Future.successful(SimpleTextResult(event, None, msg, forcePrivateResponse = true))
  }

}
