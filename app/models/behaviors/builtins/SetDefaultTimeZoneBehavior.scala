package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.MessageEvent
import services.{AWSLambdaService, DataService}
import utils.TimeZoneParser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class SetDefaultTimeZoneBehavior(
                                       tzString: String,
                                       event: MessageEvent,
                                       lambdaService: AWSLambdaService,
                                       dataService: DataService
                                     ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    val maybeTz = TimeZoneParser.maybeZoneFor(tzString)
    maybeTz.map { tz =>
      for {
        maybeTeam <- dataService.teams.find(event.teamId)
        maybeWithZone <- maybeTeam.map { team =>
          dataService.teams.setTimeZoneFor(team, tz).map(Some(_))
        }.getOrElse(Future.successful(None))
      } yield {
        maybeWithZone.map { team =>
          SimpleTextResult(event, s"OK, the default time zone for this team is now `${team.timeZone.toString}`", forcePrivateResponse = false)
        }.getOrElse {
          SimpleTextResult(event, "There was a problem trying to set the time zone for this team", forcePrivateResponse = false)
        }
      }
    }.getOrElse {
      Future.successful(
        SimpleTextResult(event, s"Sorry, I couldn't recognize the time zone `$tzString`", forcePrivateResponse = false)
      )
    }
  }

}
