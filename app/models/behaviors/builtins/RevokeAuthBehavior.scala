package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.Normal
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class RevokeAuthBehavior(
                               appName: String,
                               event: Event,
                               services: DefaultServices
                             ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val dataService = services.dataService
    for {
      maybeTeam <- dataService.teams.find(event.ellipsisTeamId)
      user <- event.ensureUser(dataService)
      maybeApplication <- maybeTeam.map { team =>
        dataService.oauth2Applications.allUsableFor(team)
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
      SimpleTextResult(event, None, msg, responseType = Normal)
    }
  }

}
