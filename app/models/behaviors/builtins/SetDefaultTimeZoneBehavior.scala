package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}
import utils.TimeZoneParser

import scala.concurrent.{ExecutionContext, Future}


case class SetDefaultTimeZoneBehavior(
                                       tzString: String,
                                       event: Event,
                                       lambdaService: AWSLambdaService,
                                       dataService: DataService
                                     ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val maybeTz = TimeZoneParser.maybeZoneFor(tzString)
    maybeTz.map { tz =>
      for {
        maybeTeam <- dataService.teams.find(event.teamId)
        maybeWithZone <- maybeTeam.map { team =>
          dataService.teams.setTimeZoneFor(team, tz).map(Some(_))
        }.getOrElse(Future.successful(None))
      } yield {
        maybeWithZone.map { team =>
          SimpleTextResult(event, None, s"OK, the default time zone for this team is now `${team.timeZone.toString}`", forcePrivateResponse = false)
        }.getOrElse {
          SimpleTextResult(event, None, "There was a problem trying to set the time zone for this team", forcePrivateResponse = false)
        }
      }
    }.getOrElse {
      Future.successful(
        SimpleTextResult(event, None, s"Sorry, I couldn't recognize the time zone `$tzString`", forcePrivateResponse = false)
      )
    }
  }

}
