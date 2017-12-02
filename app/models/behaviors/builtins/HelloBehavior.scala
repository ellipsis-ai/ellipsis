package models.behaviors.builtins

import java.util.Calendar

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class HelloBehavior(
                          event: Event,
                          services: DefaultServices
                        ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    event.messageInfo(services).map { message =>
      val userName: String = (message.details \ "name").as[String]
      val reply = s"Hello @$userName"
      SimpleTextResult(event, None, reply , forcePrivateResponse = true)
    }
  }

}
