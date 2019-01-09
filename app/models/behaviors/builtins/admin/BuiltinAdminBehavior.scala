package models.behaviors.builtins.admin

import akka.actor.ActorSystem
import models.behaviors.{BotResult, NoResponseForBuiltinResult}
import models.behaviors.builtins.BuiltinBehavior
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

trait BuiltinAdminBehavior extends BuiltinBehavior {
  lazy val dataService: DataService = services.dataService

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      isAdmin <- dataService.users.isAdmin(user)
      result <- if (isAdmin) {
        adminResult
      } else {
        Future.successful(NoResponseForBuiltinResult(event))
      }
    } yield result
  }

  protected def adminResult(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult]

  protected def teamLinkFor(teamId: String): String = {
    services.lambdaService.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      s"${baseUrl}${controllers.routes.ApplicationController.index(Some(teamId)).url}"
    }.get
  }
}
