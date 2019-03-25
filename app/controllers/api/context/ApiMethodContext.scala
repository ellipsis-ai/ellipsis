package controllers.api.context

import akka.actor.ActorSystem
import controllers.api.APIResponder
import controllers.api.json.Formatting._
import controllers.api.json._
import json.APIErrorData
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.{BotResult, BotResultService}
import models.team.Team
import play.api.Logger
import play.api.http.{HttpEntity, MimeTypes}
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.mvc.Http
import services.{DataService, DefaultServices}
import utils.FileMap

import scala.concurrent.{ExecutionContext, Future}

trait ApiMethodContext extends InjectedController with I18nSupport {

  val services: DefaultServices
  val botResultService: BotResultService = services.botResultService
  val dataService: DataService = services.dataService
  val ws: WSClient = services.ws
  val fileMap: FileMap = services.fileMap
  val eventHandler: EventHandler = services.eventHandler
  val responder: APIResponder
  implicit val ec: ExecutionContext
  implicit val actorSystem: ActorSystem
  val maybeInvocationToken: Option[InvocationToken]
  val maybeUser: Option[User]
  val maybeScheduledMessage: Option[ScheduledMessage]
  val maybeTeam: Option[Team]
  val isInvokedExternally: Boolean
  val requiresChannel: Boolean

  lazy val eventType: EventType = if (isInvokedExternally) { EventType.externalApi } else { EventType.api }

  def maybeOriginatingBehaviorVersion: Future[Option[BehaviorVersion]] = {
    maybeInvocationToken.map { invocationToken =>
      dataService.behaviorVersions.findWithoutAccessCheck(invocationToken.behaviorVersionId)
    }.getOrElse(Future.successful(None))
  }

  def maybeBehaviorVersionFor(actionName: String, maybeOriginatingBehaviorVersion: Option[BehaviorVersion]) = {
    for {
      maybeGroupVersion <- Future.successful(maybeOriginatingBehaviorVersion.map(_.groupVersion))
      maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
        dataService.behaviorVersions.findByName(actionName, groupVersion)
      }.getOrElse(Future.successful(None))
    } yield maybeBehaviorVersion
  }

  def maybeMessageEventFor(
                            message: String,
                            maybeChannel: Option[String],
                            maybeOriginalEventType: Option[EventType],
                            maybeOriginalMessageId: Option[String]
                          ): Future[Option[Event]]

  def maybeRunEventFor(
                   behaviorVersion: BehaviorVersion,
                   argumentsMap: Map[String, String],
                   maybeChannel: Option[String],
                   eventType: EventType,
                   maybeOriginalEventType: Option[EventType],
                   maybeTriggeringMessageId: Option[String]
                 ): Future[Option[RunEvent]]

  def maybeRunEventForName(
                            actionName: String,
                            argumentsMap: Map[String, String],
                            maybeChannel: Option[String],
                            maybeOriginalEventType: Option[EventType],
                            maybeOriginatingBehaviorVersion: Option[BehaviorVersion],
                            maybeOriginalMessageId: Option[String]
                          ): Future[Option[Event]] = {
    for {
      maybeBehaviorVersion <- maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      maybeEvent <- maybeBehaviorVersion.map { behaviorVersion =>
        maybeRunEventFor(behaviorVersion, argumentsMap, maybeChannel, eventType, maybeOriginalEventType, maybeOriginalMessageId)
      }.getOrElse(Future.successful(None))
    } yield maybeEvent
  }

  protected def sendFor(event: Event, result: BotResult, eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]): Future[Unit] = {
    botResultService.sendIn(result, None).map { _ =>
      Logger.info(event.logTextFor(result, Some("in response to API run request")))
    }
  }

  def runBehaviorFor(
                      maybeEvent: Option[Event],
                      eitherOriginatingBehaviorOrTriggerText: Either[BehaviorVersion, String]
                    ) = {
    for {
      result <- maybeEvent.map { event =>
        for {
          result <- eventHandler.handle(event, None).map { results =>
            results.foreach { result =>
              sendFor(event, result, eitherOriginatingBehaviorOrTriggerText)
            }
            Ok(Json.toJson(results.map(_.fullText)))
          }
        } yield result
      }.getOrElse {
        printEventCreationError()
        Future.successful(InternalServerError("Request failed.\n"))
      }
    } yield result
  }

  def runByName(
                 actionName: String,
                 info: RunActionInfo
               )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeOriginatingBehaviorVersion <- maybeOriginatingBehaviorVersion
      maybeBehaviorVersion <- maybeBehaviorVersionFor(actionName, maybeOriginatingBehaviorVersion)
      maybeEvent <- maybeRunEventForName(
        actionName,
        info.argumentsMap,
        info.maybeChannel,
        info.originalEventType.flatMap(EventType.find),
        maybeOriginatingBehaviorVersion,
        info.originalMessageId
      )
      result <- (for {
        originatingBehaviorVersion <- maybeOriginatingBehaviorVersion
        behaviorVersion <- maybeBehaviorVersion
      } yield {
        runBehaviorFor(maybeEvent, Left(originatingBehaviorVersion))
      }).getOrElse {
        Future.successful(responder.notFound(APIErrorData(s"Action named `$actionName` not found", Some("actionName")), Json.toJson(info)))
      }
    } yield result
  }

  def runByTrigger(
                    trigger: String,
                    info: RunActionInfo
                  )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeEvent <- maybeMessageEventFor(trigger, info.maybeChannel, EventType.maybeFrom(info.originalEventType), info.originalMessageId)
      result <- runBehaviorFor(maybeEvent, Right(trigger))
    } yield result
  }

  val mediumText: String

  def notSupportedResult(apiMethod: String, details: JsValue)(implicit r: Request[AnyContent]): Future[Result] = {
    val message = s"This API method ($apiMethod) is not supported for ${mediumText}"
    Future.successful(responder.badRequest(Some(APIErrorData(message, None)), None, details))
  }

  def scheduleByName(
                      actionName: String,
                      info: ScheduleActionInfo
                    )(implicit request: Request[AnyContent]): Future[Result] = notSupportedResult("scheduling by name", Json.toJson(info))

  def scheduleByTrigger(
                         trigger: String,
                         info: ScheduleActionInfo
                       )(implicit request: Request[AnyContent]): Future[Result] = notSupportedResult("schedule by trigger", Json.toJson(info))

  def unscheduleByName(
                        actionName: String,
                        info: UnscheduleActionInfo
                      )(implicit request: Request[AnyContent]): Future[Result] = notSupportedResult("unscheduled by name", Json.toJson(info))

  def unscheduleByTrigger(
                           trigger: String,
                           info: UnscheduleActionInfo
                         )(implicit request: Request[AnyContent]): Future[Result] = notSupportedResult("unschedule by trigger", Json.toJson(info))

  def addMessageListener(
                          info: AddMessageListenerInfo
                        )(implicit request: Request[AnyContent]): Future[Result] = notSupportedResult("add message listener", Json.toJson(info))

  def printEventCreationError(): Unit

  private def contentDispositionForContentType(contentType: String): String = {
    val extension = """image/(.*)""".r.findFirstMatchIn(contentType).flatMap { r =>
      r.subgroups.headOption
    }.orElse {
      if (contentType == MimeTypes.BINARY) {
        Some("jpg")
      } else {
        None
      }
    }.getOrElse("txt")
    s"""attachment; filename="ellipsis.${extension}""""
  }

  def getFileFetchToken: Future[String]

  def fetchFileResultFor(fileId: String)(implicit r: Request[AnyContent]): Future[Result] = {
    fileMap.maybeUrlToUseFor(fileId).flatMap { maybeUrl =>
      maybeUrl.map { urlToUse =>
        for {
          token <- getFileFetchToken
          httpHeaders <- Future.successful(Seq(
            (Http.HeaderNames.AUTHORIZATION -> s"Bearer $token")
          ))
          result <- ws.url(urlToUse).withHttpHeaders(httpHeaders: _*).withMethod("GET").stream().map { r =>
            if (r.status == 200) {
              val contentType =
                r.headers.get(CONTENT_TYPE).
                  flatMap(_.headOption).
                  getOrElse(MimeTypes.BINARY)

              val result = r.headers.get(CONTENT_LENGTH) match {
                case Some(Seq(length)) =>
                  Ok.sendEntity(HttpEntity.Streamed(r.bodyAsSource, Some(length.toLong), Some(contentType)))
                case _ =>
                  Ok.chunked(r.bodyAsSource).as(contentType)
              }
              val contentDisposition = r.headers.get(CONTENT_DISPOSITION).flatMap(_.headOption).getOrElse {
                contentDispositionForContentType(contentType)
              }
              val headers = Seq((CONTENT_DISPOSITION -> contentDisposition))
              result.withHeaders(headers: _*)

            } else {
              BadGateway
            }
          }
        } yield result
      }.getOrElse(Future.successful(NotFound(s"Unable to find a file with ID $fileId")))
    }

  }

}
