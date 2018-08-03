package models.behaviors.builtins

import java.util.Calendar

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class HelloBehavior(
                          event: Event,
                          services: DefaultServices
                        ) extends BuiltinBehavior {

  private def version(services: DefaultServices): Future[Option[String]] = {
    services.dataService.teams.allTeams.map { teams =>
      services.configuration.getOptional[String]("application.version").map(Some(_)).getOrElse(None)
    }
  }

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      messageInfo <- event.messageInfo(None, services)
      appVersion <- version(services)
    } yield {
      val greeting = (messageInfo.details \ "name").asOpt[String].map(name => (s"Hello @$name")).getOrElse("Hello")
      val message = appVersion.map(v => (s"This is Ellipsis version $v")).getOrElse("This is Ellipsis, I am ready to help.")
      val reply = s"$greeting\n$message"
      SimpleTextResult(event, None, reply , forcePrivateResponse = true)
    }
  }

}
