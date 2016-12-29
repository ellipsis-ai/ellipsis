package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, seq}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Result}
import play.utils.UriEncoding
import services.slack.SlackMessageEvent
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

  case class EventInfo(
                  eventType: String,
                  ts: String,
                  userId: String,
                  channel: String,
                  text: String
                  )

  case class EventRequestInfo(
                        token: String,
                        teamId: String,
                        apiAppId: String,
                        event: EventInfo,
                        requestType: String,
                        authedUsers: Seq[String]
                      ) extends RequestInfo

  private val eventRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "api_app_id" -> nonEmptyText,
      "event" -> mapping(
        "type" -> nonEmptyText,
        "ts" -> nonEmptyText,
        "user" -> nonEmptyText,
        "channel" -> nonEmptyText,
        "text" -> nonEmptyText
      )(EventInfo.apply)(EventInfo.unapply),
      "type" -> nonEmptyText,
      "authed_users" -> seq(nonEmptyText)
    )(EventRequestInfo.apply)(EventRequestInfo.unapply) verifying("Not an event request", fields => fields match {
      case info => info.requestType == "event_callback"
    })
  )

  private def eventResult(info: EventRequestInfo): Result = {
    if (info.isValid) {
      for {
        maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(info.teamId).map(_.headOption)
        _ <- maybeProfile.map { profile =>
          val event = info.event
          slackEventService.onEvent(SlackMessageEvent(profile, event.channel, event.userId, event.text.trim, event.ts))
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
      formWithErrors => {
        eventRequestForm.bindFromRequest.fold(
          formWithErrors => {
            BadRequest(formWithErrors.errorsAsJson)
          },
          info => eventResult(info)
        )
      },
      info => challengeResult(info)
    )
  }


}
