package controllers.webhooks.chargebee


import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{EllipsisController, ReAuthable, RemoteAssets}
import models.silhouette.EllipsisEnv
import play.api.{Configuration, Logger}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class EventsController @Inject() (
                                   val silhouette: Silhouette[EllipsisEnv],
                                   val dataService: DataService,
                                   val configuration: Configuration,
                                   val assetsProvider: Provider[RemoteAssets],
                                   implicit val ec: ExecutionContext
                                 ) extends EllipsisController {

  def create = Action.async { implicit request =>
    Logger.info(s"Chargebee Event: ${request.body.asJson.get.toString()}")
    Future.successful(Ok(""))
  }


}


