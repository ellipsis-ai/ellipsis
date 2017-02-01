package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.events.SlackMessageEvent
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Result}
import play.utils.UriEncoding
import services.{AWSLambdaService, DataService, SlackEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SlackController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val configuration: Configuration,
                                  val dataService: DataService,
                                  val slackEventService: SlackEventService,
                                  val lambdaService: AWSLambdaService,
                                  val cache: CacheApi,
                                  val ws: WSClient,
                                  implicit val actorSystem: ActorSystem
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

      // respond immediately
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

  case class ActionInfo(name: String, value: String, text: Option[String])
  case class TeamInfo(id: String, domain: String)
  case class ChannelInfo(id: String, name: String)
  case class UserInfo(id: String, name: String)
  case class OriginalMessageInfo(text: String, attachments: Seq[AttachmentInfo])
  case class AttachmentInfo(
                             title: Option[String],
                             text: Option[String],
                             mrkdwn_in: Option[Seq[String]],
                             fields: Option[Seq[FieldInfo]] = None,
                             actions: Option[Seq[ActionInfo]] = None,
                             color: Option[String] = None
                           )
  case class FieldInfo(title: Option[String], value: Option[String])
  case class ActionsTriggeredInfo(
                                   callback_id: String,
                                   actions: Seq[ActionInfo],
                                   team: TeamInfo,
                                   channel: ChannelInfo,
                                   user: UserInfo,
                                   action_ts: String,
                                   message_ts: String,
                                   attachment_id: String,
                                   token: String,
                                   original_message: OriginalMessageInfo,
                                   response_url: String
                                 ) extends RequestInfo {

    def maybeConversationIdToStart: Option[String] = {
      actions.find { info => info.name == "start_conversation" && info.value == "true" }.map { _ =>
        callback_id
      }
    }

    def maybeHelpForSkillId: Option[String] = {
      actions.find { info => info.name == "help_for_skill" && !info.value.isEmpty }.map { info =>
        info.value
      }
    }

    def maybeHelpIndex: Option[String] = {
      actions.find { info => info.name == "help_index" }.map { _ => "index" }
    }

  }

  private val actionForm = Form(
    "payload" -> nonEmptyText
  )

  implicit val channelReads = Json.reads[ChannelInfo]
  implicit val teamReads = Json.reads[TeamInfo]
  implicit val userReads = Json.reads[UserInfo]
  implicit val actionReads = Json.reads[ActionInfo]
  implicit val fieldReads = Json.reads[FieldInfo]
  implicit val attachmentReads = Json.reads[AttachmentInfo]
  implicit val messageReads = Json.reads[OriginalMessageInfo]
  implicit val actionWrites = Json.writes[ActionInfo]
  implicit val fieldWrites = Json.writes[FieldInfo]
  implicit val attachmentWrites = Json.writes[AttachmentInfo]
  implicit val messageWrites = Json.writes[OriginalMessageInfo]
  implicit val actionsTriggeredReads = Json.reads[ActionsTriggeredInfo]

  def eventForSlackBot(info: ActionsTriggeredInfo): Future[Option[SlackMessageEvent]] = {
    dataService.slackBotProfiles.allForSlackTeamId(info.team.id).map { botProfiles =>
      botProfiles.headOption.map { botProfile =>
        SlackMessageEvent(botProfile, info.channel.id, None, info.user.id, "", info.message_ts)
      }
    }
  }

  def action = Action { implicit request =>
    actionForm.bindFromRequest.fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      payload => {
        Json.parse(payload).validate[ActionsTriggeredInfo] match {
          case JsSuccess(info, jsPath) => {
            if (info.isValid) {
              var resultText: String = "_OK, let’s continue._"

              info.maybeHelpIndex.foreach { _ =>
                eventForSlackBot(info).map { maybeEvent =>
                  maybeEvent.map { event =>
                    DisplayHelpBehavior(None, None, isFirstTrigger = false, event, lambdaService, dataService).result.flatMap(result => result.sendIn(None, None))
                  }.getOrElse(Future.successful({}))
                }
                resultText = "_You clicked *Other help*._"
              }

              info.maybeHelpForSkillId.foreach { skillId =>
                eventForSlackBot(info).map { maybeEvent =>
                  maybeEvent.map { event =>
                    val result = if (skillId == "(untitled)") {
                      DisplayHelpBehavior(Some(skillId), None, isFirstTrigger = false, event, lambdaService, dataService).result
                    } else {
                      DisplayHelpBehavior(None, Some(skillId), isFirstTrigger = false, event, lambdaService, dataService).result
                    }
                    result.flatMap(result => result.sendIn(None, None))
                  }.getOrElse(Future.successful({}))
                }
                resultText = info.original_message.attachments.flatMap(_.actions).flatten.filter { action =>
                  action.name == "help_for_skill" && action.value == skillId
                }.flatMap(_.text).headOption.map(buttonLabel => s"_You clicked *$buttonLabel.*_").getOrElse("_You clicked a button._")
              }

              // respond immediately by removing buttons and appending a new attachment
              val attachmentsWithoutButtons = info.original_message.attachments.filter(ea => ea.text.isDefined).map(ea => ea.copy(actions = None))
              val resultAttachment = AttachmentInfo(title = None, text = Some(resultText), mrkdwn_in = Some(Seq("text")), color = Some("#C3CCFE"))
              val updated = info.original_message.copy(attachments = attachmentsWithoutButtons :+ resultAttachment)
              Ok(Json.toJson(updated))
            } else {
              Unauthorized("Bad token")
            }
          }
          case JsError(err) => {
            BadRequest(err.toString)
          }
        }
      }
    )
  }


}
