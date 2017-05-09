package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RevokeAuthBehavior(
                               appName: String,
                               event: Event,
                               lambdaService: AWSLambdaService,
                               dataService: DataService
                             ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      user <- event.ensureUser(dataService)
      maybeApplication <- maybeTeam.map { team =>
        dataService.oauth2Applications.allFor(team)
      }.getOrElse(Future.successful(Seq())).map(all => all.find(_.name.toLowerCase == appName.toLowerCase))
      didDelete <- maybeApplication.map { app =>
        dataService.linkedOAuth2Tokens.deleteFor(app, user)
      }.getOrElse(Future.successful(false))
    } yield {
      val msg = if (didDelete) {
        s"OK, I revoked all tokens for you for `$appName`"
      } else {
        s"I couldn't find any auth tokens for you for `$appName`"
      }
      SimpleTextResult(event, None, msg, forcePrivateResponse = false)
    }
  }

}
