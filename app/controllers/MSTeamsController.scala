package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.behaviors.events._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, seq}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.{Environment, Logger, Mode}
import play.utils.UriEncoding
import services._

import scala.concurrent.{ExecutionContext, Future}

class MSTeamsController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val eventHandler: EventHandler,
                                  val services: DefaultServices,
                                  val assetsProvider: Provider[RemoteAssets],
                                  val environment: Environment,
                                  implicit val ec: ExecutionContext
                                ) extends EllipsisController {

  val dataService = services.dataService
  val configuration = services.configuration
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  val ws = services.ws
  val botResultService = services.botResultService
  implicit val actorSystem = services.actorSystem

//  {
//    "type" : "message",
//    "id" : "2107519c57b848e1a0f475eb9a3bafe0|0000001",
//    "timestamp" : "2018-11-06T19:02:00.8303001Z",
//    "serviceUrl" : "https://webchat.botframework.com/",
//    "channelId" : "webchat",
//    "from" : {
//      "id" : "CmJNRcv6XjM",
//      "name" : "You"
//    },
//    "conversation" : {
//      "id" : "2107519c57b848e1a0f475eb9a3bafe0"
//    },
//    "recipient" : {
//      "id" : "EllipsisAndrewDev@29pdMRjysi8",
//      "name" : "EllipsisAndrewDev"
//    },
//    "textFormat" : "plain",
//    "locale" : "en",
//    "text" : "hi",
//    "channelData" : {
//      "clientActivityId" : "1541530451777.19835681506493863.4"
//    }
//  }

  case class MessageParticipantInfo(id: String, name: String)

  case class ConversationInfo(id: String)

  case class TenantInfo(id: String)

  case class ChannelDataInfo(
                              clientActivityId: Option[String],
                              tenant: Option[TenantInfo]
                            )

  case class ActivityInfo(
                           activityType: String,
                           id: String,
                           timestamp: String,
                           serviceUrl: String,
                           channelId: String,
                           from: MessageParticipantInfo,
                           conversation: ConversationInfo,
                           recipient: MessageParticipantInfo,
                           textFormat: String,
                           locale: Option[String],
                           text: String,
                           channelData: ChannelDataInfo
                         ) {
    val responseUrl: String = s"$serviceUrl/v3/conversations/${conversation.id}/activities/${id}"
  }

  lazy implicit val messageParticipantFormat = Json.format[MessageParticipantInfo]
  lazy implicit val conversationFormat = Json.format[ConversationInfo]
  lazy implicit val tenantFormat = Json.format[TenantInfo]
  lazy implicit val channelDataFormat = Json.format[ChannelDataInfo]
  lazy implicit val activityFormat = Json.format[ActivityInfo]

  private val messageParticipantMapping = mapping(
    "id" -> nonEmptyText,
    "name" -> nonEmptyText
  )(MessageParticipantInfo.apply)(MessageParticipantInfo.unapply)

  private val messageActivityForm = Form(
    mapping(
      "type" -> nonEmptyText,
      "id" -> nonEmptyText,
      "timestamp" -> nonEmptyText,
      "serviceUrl" -> nonEmptyText,
      "channelId" -> nonEmptyText,
      "from" -> messageParticipantMapping,
      "conversation" -> mapping(
        "id" -> nonEmptyText
      )(ConversationInfo.apply)(ConversationInfo.unapply),
      "recipient" -> messageParticipantMapping,
      "textFormat" -> nonEmptyText,
      "locale" -> optional(nonEmptyText),
      "text" -> nonEmptyText,
      "channelData" -> mapping(
        "clientActivityId" -> optional(nonEmptyText),
        "tenant" -> optional(mapping(
          "id" -> nonEmptyText
        )(TenantInfo.apply)(TenantInfo.unapply))
      )(ChannelDataInfo.apply)(ChannelDataInfo.unapply)
    )(ActivityInfo.apply)(ActivityInfo.unapply) verifying ("Not a valid message event", fields => fields match {
      case info => info.activityType == "message"
    })
  )

  case class ResponseInfo(
                           `type`: String,
                           from: MessageParticipantInfo,
                           conversation: ConversationInfo,
                           recipient: MessageParticipantInfo,
                           text: String,
                           replyToId: String
                         )

  lazy implicit val responseFormat = Json.format[ResponseInfo]

  def respondTo(info: ActivityInfo): Future[Unit] = {
    val response = ResponseInfo(
      "message",
      info.recipient,
      info.conversation,
      info.from,
      "I received: " ++ info.text,
      info.id
    )
    ws.url(info.responseUrl).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Json.toJson(response)).
      map { res =>
        Logger.info(s"MS Teams responded:\n${Json.prettyPrint(res.json)}")
      }
  }

  def event = Action { implicit request =>
    if (environment.mode == Mode.Dev) {
      Logger.info(s"MS Teams event received:\n${Json.prettyPrint(request.body.asJson.get)}")
    }
    messageActivityForm.bindFromRequest.fold(
      errors => {
        Logger.info(s"BadRequest:\n${Json.prettyPrint(errors.errorsAsJson)}")
        BadRequest("")
      },
      info => respondTo(info)
    )

    Ok("Got it!")
  }

  def signIn(maybeRedirectUrl: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    val eventualMaybeTeamAccess = request.identity.map { user =>
      dataService.users.teamAccessFor(user, None).map(Some(_))
    }.getOrElse(Future.successful(None))
    eventualMaybeTeamAccess.map { maybeTeamAccess =>
      val maybeResult = for {
        scopes <- configuration.getOptional[String]("silhouette.ms_teams.scope")
        clientId <- configuration.getOptional[String]("silhouette.ms_teams.clientID")
      } yield {
        val redirectUrl = routes.SocialAuthController.authenticateMSTeams(maybeRedirectUrl).absoluteURL(secure=true)
        Ok(views.html.slack.signInWithSlack(viewConfig(maybeTeamAccess), scopes, clientId, UriEncoding.encodePathSegment(redirectUrl, "utf-8")))
      }
      maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
    }
  }

}
