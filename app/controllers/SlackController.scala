package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import models.behaviors.BehaviorResponse
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events.{EventHandler, SlackMessageEvent}
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import play.utils.UriEncoding
import services.{AWSLambdaService, DataService, SlackEventService}
import utils.SlackMessageReactionHandler

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
                                  val eventHandler: EventHandler,
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

  private def messageEventResult(info: MessageRequestInfo)(implicit request: Request[AnyContent]): Result = {
    if (info.isValid) {
      val isRetry = request.headers.get("X-Slack-Retry-Num").isDefined
      if (isRetry) {
        Ok("We are ignoring retries for now")
      } else {
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
      }
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
              _ => Ok("I don't know what to do with this request but I'm not concerned"),
              info => messageEventResult(info)
            )
          },
          info => messageEventResult(info)
        )
      },
      info => challengeResult(info)
    )
  }

  case class ActionSelectOptionInfo(text: Option[String], value: String)
  case class ActionTriggeredInfo(name: String, value: Option[String], selected_options: Option[Seq[ActionSelectOptionInfo]])
  case class ActionInfo(
                         name: String,
                         text: String,
                         value: Option[String],
                         `type`: String,
                         style: Option[String],
                         options: Option[Seq[ActionSelectOptionInfo]],
                         selected_options: Option[Seq[ActionSelectOptionInfo]]
                       )
  case class TeamInfo(id: String, domain: String)
  case class ChannelInfo(id: String, name: String)
  case class UserInfo(id: String, name: String)
  case class OriginalMessageInfo(text: String, attachments: Seq[AttachmentInfo], response_type: Option[String], replace_original: Option[Boolean])
  case class AttachmentInfo(
                             fallback: Option[String] = None,
                             title: Option[String] = None,
                             text: Option[String] = None,
                             mrkdwn_in: Option[Seq[String]] = None,
                             callback_id: Option[String] = None,
                             fields: Option[Seq[FieldInfo]] = None,
                             actions: Option[Seq[ActionInfo]] = None,
                             color: Option[String] = None,
                             title_link: Option[String] = None,
                             pretext: Option[String] = None,
                             author_name: Option[String] = None,
                             author_icon: Option[String] = None,
                             author_link: Option[String] = None,
                             image_url: Option[String] = None,
                             thumb_url: Option[String] = None,
                             footer: Option[String] = None,
                             footer_icon: Option[String] = None,
                             ts: Option[String] = None
                           )
  case class FieldInfo(title: Option[String], value: Option[String], short: Option[Boolean] = None)
  case class ActionsTriggeredInfo(
                                   callback_id: String,
                                   actions: Seq[ActionTriggeredInfo],
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

    def maybeHelpForSkillIdWithMaybeSearch: Option[HelpGroupSearchValue] = {
      actions.find(_.name == SHOW_BEHAVIOR_GROUP_HELP).flatMap {
        _.value.map { value =>
          HelpGroupSearchValue.fromString(value)
        }
      }
    }

    def maybeActionListForSkillId: Option[HelpGroupSearchValue] = {
      actions.find(_.name == LIST_BEHAVIOR_GROUP_ACTIONS).flatMap {
        _.value.map { value =>
          HelpGroupSearchValue.fromString(value)
        }
      }
    }

    def maybeHelpIndexAt: Option[Int] = {
      actions.find { info => info.name == SHOW_HELP_INDEX }.map { _.value.map { value =>
        try {
          value.toInt
        } catch {
          case _: NumberFormatException => 0
        }
      }.getOrElse(0) }
    }

    def maybeConfirmContinueConversationId: Option[String] = {
      actions.find(_.name == CONFIRM_CONTINUE_CONVERSATION).flatMap(_.value)
    }

    def maybeDontContinueConversationId: Option[String] = {
      actions.find(_.name == DONT_CONTINUE_CONVERSATION).flatMap(_.value)
    }

    def maybeStopConversationId: Option[String] = {
      actions.find(_.name == STOP_CONVERSATION).flatMap(_.value)
    }

    def maybeRunBehaviorVersionId: Option[String] = {
      val maybeAction = actions.find(_.name == RUN_BEHAVIOR_VERSION)
      val maybeValue = maybeAction.flatMap(_.value)
      maybeValue.orElse {
        for {
          selectedOptions <- maybeAction.map(_.selected_options)
          firstOption <- selectedOptions.map(_.headOption)
          behaviorId <- firstOption.map(_.value)
        } yield {
          behaviorId
        }
      }
    }

    def maybeFutureEvent: Future[Option[SlackMessageEvent]] = {
      dataService.slackBotProfiles.allForSlackTeamId(this.team.id).map { botProfiles =>
        botProfiles.headOption.map { botProfile =>
          SlackMessageEvent(botProfile, this.channel.id, None, this.user.id, "", this.message_ts)
        }
      }
    }

    def findOptionLabelForValue(value: String): Option[String] = {
      for {
        attachment <- this.original_message.attachments.headOption
        actions <- attachment.actions
        select <- actions.find(_.`type` == "select")
        options <- select.options
        matchingOption <- options.find(_.value == value)
        text <- matchingOption.text
      } yield {
        text
      }
    }

    def findButtonLabelForNameAndValue(name: String, value: String): Option[String] = {
      val actions = this.original_message.attachments.flatMap(_.actions).flatten
      val maybeAction = actions.find(action => action.`type` == "button" && action.name == name && action.value.exists { actionValue =>
        actionValue == value || HelpGroupSearchValue.fromString(actionValue).helpGroupId == value
      })
      maybeAction.map(_.text)
    }
  }

  private val actionForm = Form(
    "payload" -> nonEmptyText
  )

  implicit val channelReads = Json.reads[ChannelInfo]
  implicit val teamReads = Json.reads[TeamInfo]
  implicit val userReads = Json.reads[UserInfo]

  implicit val actionSelectOptionReads = Json.reads[ActionSelectOptionInfo]
  implicit val actionSelectOptionWrites = Json.writes[ActionSelectOptionInfo]

  implicit val actionReads = Json.reads[ActionInfo]
  implicit val actionWrites = Json.writes[ActionInfo]

  implicit val actionTriggeredReads = Json.reads[ActionTriggeredInfo]

  implicit val fieldReads = Json.reads[FieldInfo]
  implicit val fieldWrites = Json.writes[FieldInfo]

  implicit val attachmentReads = Json.reads[AttachmentInfo]
  implicit val attachmentWrites = Json.writes[AttachmentInfo]

  implicit val messageReads = Json.reads[OriginalMessageInfo]
  implicit val messageWrites = Json.writes[OriginalMessageInfo]

  implicit val actionsTriggeredReads = Json.reads[ActionsTriggeredInfo]

  def action = Action { implicit request =>
    actionForm.bindFromRequest.fold(
      formWithErrors => {
        println(formWithErrors.errorsAsJson)
        BadRequest(formWithErrors.errorsAsJson)
      },
      payload => {

        // Slack improperly (?) displays escaped text inside button labels as-is in the client when
        // we return the original message back.
        //
        // TODO: Investigate whether this is safe and/or desirable
        val unescapedPayload = payload.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")

        Json.parse(unescapedPayload).validate[ActionsTriggeredInfo] match {
          case JsSuccess(info, jsPath) => {
            if (info.isValid) {
              var resultText: String = "OK, let’s continue."
              var shouldRemoveActions = false
              val user = s"<@${info.user.id}>"

              info.maybeHelpIndexAt.foreach { index =>
                info.maybeFutureEvent.flatMap { maybeEvent =>
                  maybeEvent.map { event =>
                    DisplayHelpBehavior(
                      None,
                      None,
                      Some(index),
                      includeNameAndDescription = false,
                      includeNonMatchingResults = false,
                      isFirstTrigger = false,
                      event,
                      lambdaService,
                      dataService
                    ).result.flatMap(result => result.sendIn(None, dataService))
                  }.getOrElse(Future.successful({}))
                }.recover {
                  case t: Throwable => {
                    Logger.error("Exception responding to a Slack action for help index", t)
                  }
                }
                resultText = s"$user clicked More help."
              }

              info.maybeHelpForSkillIdWithMaybeSearch.foreach { searchValue =>
                info.maybeFutureEvent.flatMap { maybeEvent =>
                  maybeEvent.map { event =>
                    val result = DisplayHelpBehavior(
                      searchValue.maybeSearchText,
                      Some(searchValue.helpGroupId),
                      None,
                      includeNameAndDescription = true,
                      includeNonMatchingResults = false,
                      isFirstTrigger = false,
                      event,
                      lambdaService,
                      dataService
                    ).result
                    result.flatMap(result => result.sendIn(None, dataService))
                  }.getOrElse(Future.successful({}))
                }.recover {
                  case t: Throwable => {
                    Logger.error("Exception responding to a Slack action for skill help with maybe search", t)
                  }
                }
                resultText = info.findButtonLabelForNameAndValue(SHOW_BEHAVIOR_GROUP_HELP, searchValue.helpGroupId).map { text =>
                  s"$user clicked $text."
                } getOrElse {
                  s"$user clicked a button."
                }
              }

              info.maybeActionListForSkillId.foreach { searchValue =>
                info.maybeFutureEvent.flatMap { maybeEvent =>
                  maybeEvent.map { event =>
                    val result = DisplayHelpBehavior(
                      searchValue.maybeSearchText,
                      Some(searchValue.helpGroupId),
                      None,
                      includeNameAndDescription = false,
                      includeNonMatchingResults = true,
                      isFirstTrigger = false,
                      event,
                      lambdaService,
                      dataService
                    ).result
                    result.flatMap(result => result.sendIn(None, dataService))
                  }.getOrElse(Future.successful({}))
                }.recover {
                  case t: Throwable => {
                    Logger.error("Exception responding to a Slack action for skill action list", t)
                  }
                }
                resultText = s"$user clicked List all actions"
              }

              info.maybeConfirmContinueConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.touch(convo).flatMap { _ =>
                      cache.get[SlackMessageEvent](convo.pendingEventKey).map { event =>
                        slackEventService.onEvent(event)
                      }.getOrElse(Future.successful({}))
                    }
                  }.getOrElse(Future.successful({}))
                }
                shouldRemoveActions = true
                resultText = s"$user clicked 'Yes'"
              }

              info.maybeDontContinueConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.background(convo, "OK, on to the next thing.", includeUsername = false).flatMap { _ =>
                      cache.get[SlackMessageEvent](convo.pendingEventKey).map { event =>
                        eventHandler.handle(event, None).flatMap { results =>
                          Future.sequence(
                            results.map(result => result.sendIn(None, dataService).map { _ =>
                              Logger.info(event.logTextFor(result))
                            })
                          )
                        }
                      }.getOrElse(Future.successful({}))
                    }
                  }.getOrElse(Future.successful({}))
                }
                shouldRemoveActions = true
                resultText = s"$user clicked 'No'"
              }

              info.maybeStopConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.cancel(convo)
                  }.getOrElse(Future.successful({}))
                }
                shouldRemoveActions = true
                resultText = s"$user stopped the conversation"
              }

              info.maybeRunBehaviorVersionId.foreach { behaviorVersionId =>
                info.maybeFutureEvent.flatMap(_.map { event =>
                  val eventualMaybeBehaviorVersion = dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId)
                  val eventualResponse = eventualMaybeBehaviorVersion.flatMap(_.map { behaviorVersion =>
                    val eventualSentResponse = BehaviorResponse.buildFor(event, behaviorVersion, Map(), None, None, lambdaService, dataService, cache, ws, configuration)
                    eventualSentResponse.flatMap(_.result.map(_.sendIn(None, dataService)))
                  }.getOrElse {
                    Future.successful({})
                  })
                  SlackMessageReactionHandler.handle(event.clientFor(dataService), eventualResponse, info.channel.id, info.message_ts, delayMilliseconds = 500)
                }.getOrElse(Future.successful({})))

                resultText = info.findButtonLabelForNameAndValue(RUN_BEHAVIOR_VERSION, behaviorVersionId).map { text =>
                  s"$user clicked $text"
                } orElse info.findOptionLabelForValue(behaviorVersionId).map { text =>
                  s"$user ran ${text.mkString("“", "", "”")}"
                } getOrElse {
                  s"$user ran an action"
                }
              }

              // respond immediately by appending a new attachment
              val maybeOriginalColor = info.original_message.attachments.headOption.flatMap(_.color)
              val newAttachment = AttachmentInfo(Some(resultText), None, None, Some(Seq("text")), Some(info.callback_id), color = maybeOriginalColor, footer = Some(resultText))
              val originalAttachmentsToUse = if (shouldRemoveActions) {
                info.original_message.attachments.map(ea => ea.copy(actions = None))
              } else {
                info.original_message.attachments
              }
              val updated = info.original_message.copy(attachments = originalAttachmentsToUse :+ newAttachment)
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
