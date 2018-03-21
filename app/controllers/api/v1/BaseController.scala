package controllers.api.v1

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import controllers.{EllipsisController, RemoteAssets}
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Request, Result}
import services.caching.CacheService
import services.{AWSLambdaService, DataService, SlackEventService}

import scala.concurrent.ExecutionContext


class BaseController @Inject() (
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 val cacheService: CacheService,
                                 val lambdaService: AWSLambdaService,
                                 val ws: WSClient,
                                 val slackService: SlackEventService,
                                 val eventHandler: EventHandler,
                                 val botResultService: BotResultService,
                                 val assetsProvider: Provider[RemoteAssets],
                                 implicit val actorSystem: ActorSystem,
                                 implicit val ec: ExecutionContext
                               )
  extends EllipsisController {

  class InvalidTokenException extends Exception

  protected def logAndRespondFor(status: Status, message: String, details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    val result = status.apply(message)
    Logger.info(
      s"""Returning a ${result.header.status} for: $message
         |
         |Api info: ${Json.prettyPrint(details)}
         |
         |Request: ${r} with ${r.rawQueryString} ${r.body}""".stripMargin)
    result
  }

  protected def badRequest(message: String, details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(BadRequest, message, details)
  }

  protected def notFound(message: String, details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(NotFound, message, details)
  }

}
