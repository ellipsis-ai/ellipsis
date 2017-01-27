package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.behaviors.events.SlackMessageEvent
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Result}
import play.utils.UriEncoding
import services.{DataService, SlackEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SlackController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val configuration: Configuration,
                                  val dataService: DataService,
                                  val slackEventService: SlackEventService
                                ) extends EllipsisController {

  def add = silhouette.UserAwareAction { implicit request =>
    val maybeResult = for {
      scopes <- configuration.getString("silhouette.slack.scope")
      clientId <- configuration.getString("silhouette.slack.clientID")
    } yield {
        val redirectUrl = routes.SocialAuthController.installForSlack().absoluteURL(secure=true)
        Ok(views.html.addToSlack(viewConfig(None), scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
  }

  def signIn(maybeRedirectUrl: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    val eventualMaybeTeamAccess = request.identity.map { user =>
      dataService.users.teamAccessFor(user, None).map(Some(_))
    }.getOrElse(Future.successful(None))
    eventualMaybeTeamAccess.map { maybeTeamAccess =>
      val maybeResult = for {
        scopes <- configuration.getString("silhouette.slack.signInScope")
        clientId <- configuration.getString("silhouette.slack.clientID")
      } yield {
          val redirectUrl = routes.SocialAuthController.authenticateSlack(maybeRedirectUrl).absoluteURL(secure=true)
          Ok(views.html.signInWithSlack(viewConfig(maybeTeamAccess), scopes, clientId, UriEncoding.encodePathSegment(redirectUrl, "utf-8")))
        }
      maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
    }
  }

  trait RequestInfo {
    val token: String
    def isValid: Boolean = configuration.getString("slack.token").contains(token)
  }

  case class ChallengeRequestInfo(token: String, challenge: String, requestType: String) extends RequestInfo

  private val challengeRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "challenge" -> nonEmptyText,
      "type" -> nonEmptyText
    )(ChallengeRequestInfo.apply)(ChallengeRequestInfo.unapply) verifying("Not a challenge", fields => fields match {
      case info => info.requestType == "url_verification"
    })
  )

  private def challengeResult(info: ChallengeRequestInfo): Result = {
    if (info.isValid) {
      Ok(info.challenge)
    } else {
      Unauthorized("Bad token")
    }
  }

  trait MessageRequestInfo extends RequestInfo {
    val teamId: String
    val channel: String
    val userId: String
    val message: String
    val ts: String
    val maybeThreadTs: Option[String]
  }

  case class MessageSentEventInfo(
                                    eventType: String,
                                    ts: String,
                                    maybeThreadTs: Option[String],
                                    userId: String,
                                    channel: String,
                                    text: String
                                  )

  case class MessageSentEventRequestInfo(
                                          token: String,
                                          teamId: String,
                                          apiAppId: String,
                                          event: MessageSentEventInfo,
                                          requestType: String,
                                          authedUsers: Seq[String]
                                        ) extends MessageRequestInfo {
    val message: String = event.text.trim
    val userId: String = event.userId
    val channel: String = event.channel
    val ts: String = event.ts
    val maybeThreadTs = event.maybeThreadTs
  }

  private val messageSentEventRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "api_app_id" -> nonEmptyText,
      "event" -> mapping(
        "type" -> nonEmptyText,
        "ts" -> nonEmptyText,
        "thread_ts" -> optional(nonEmptyText),
        "user" -> nonEmptyText,
        "channel" -> nonEmptyText,
        "text" -> nonEmptyText
      )(MessageSentEventInfo.apply)(MessageSentEventInfo.unapply),
      "type" -> nonEmptyText,
      "authed_users" -> seq(nonEmptyText)
    )(MessageSentEventRequestInfo.apply)(MessageSentEventRequestInfo.unapply) verifying("Not an event request", fields => fields match {
      case info => info.requestType == "event_callback"
    })
  )

  case class EditedInfo(user: String, ts: String)

  case class ChangedMessageInfo(
                                 eventType: String,
                                 ts: String,
                                 userId: String,
                                 text: String,
                                 edited: EditedInfo
                               )

  case class MessageChangedEventInfo(
                                      eventType: String,
                                      message: ChangedMessageInfo,
                                      eventSubType: String,
                                      channel: String,
                                      eventTs: String,
                                      maybeThreadTs: Option[String],
                                      ts: String
                                   )

  case class MessageChangedEventRequestInfo(
                                             token: String,
                                             teamId: String,
                                             apiAppId: String,
                                             event: MessageChangedEventInfo,
                                             requestType: String,
                                             authedUsers: Seq[String]
                                          ) extends MessageRequestInfo {
    val message: String = event.message.text.trim
    val userId: String = event.message.userId
    val channel: String = event.channel
    val ts: String = event.ts
    val maybeThreadTs = event.maybeThreadTs
  }

  private val messageChangedEventRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "api_app_id" -> nonEmptyText,
      "event" -> mapping(
        "type" -> nonEmptyText,
        "message" -> mapping(
          "type" -> nonEmptyText,
          "ts" -> nonEmptyText,
          "user" -> nonEmptyText,
          "text" -> nonEmptyText,
          "edited" -> mapping(
            "user" -> nonEmptyText,
            "ts" -> nonEmptyText
          )(EditedInfo.apply)(EditedInfo.unapply)
        )(ChangedMessageInfo.apply)(ChangedMessageInfo.unapply),
        "subtype" -> nonEmptyText,
        "channel" -> nonEmptyText,
        "event_ts" -> nonEmptyText,
        "thread_ts" -> optional(nonEmptyText),
        "ts" -> nonEmptyText
      )(MessageChangedEventInfo.apply)(MessageChangedEventInfo.unapply),
      "type" -> nonEmptyText,
      "authed_users" -> seq(nonEmptyText)
    )(MessageChangedEventRequestInfo.apply)(MessageChangedEventRequestInfo.unapply) verifying("Not an edited message event request", fields => fields match {
      case info => info.requestType == "event_callback" && info.event.eventSubType == "message_changed"
    })
  )

  private def messageEventResult(info: MessageRequestInfo): Result = {
    if (info.isValid) {
      for {
        maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(info.teamId).map(_.headOption)
        _ <- maybeProfile.map { profile =>
          slackEventService.onEvent(SlackMessageEvent(profile, info.channel, info.maybeThreadTs, info.userId, info.message, info.ts))
        }.getOrElse {
          Future.successful({})
        }
      } yield {}
      Ok(":+1:")
    } else {
      Unauthorized("Bad token")
    }
  }

  def event = Action { implicit request =>
    challengeRequestForm.bindFromRequest.fold(
      _ => {
        messageSentEventRequestForm.bindFromRequest.fold(
          _ => {
            messageChangedEventRequestForm.bindFromRequest.fold(
              _ => {
                Ok("I don't know what to do with this request but I'm not concerned")
              },
              info => {
                val isRetry = request.headers.get("X-Slack-Retry-Num").isDefined
                if (isRetry) {
                  Ok("We are ignoring retries for now")
                } else {
                  messageEventResult(info)
                }
              }
            )
          },
          info => messageEventResult(info)
        )
      },
      info => challengeResult(info)
    )
  }


}
