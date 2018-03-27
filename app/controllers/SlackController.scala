package controllers

import javax.inject.Inject
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import json.Formatting._
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.ActionChoice
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import play.utils.UriEncoding
import services._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SlackController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val eventHandler: EventHandler,
                                  val slackEventService: SlackEventService,
                                  val services: DefaultServices,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends EllipsisController {

  val dataService = services.dataService
  val configuration = services.configuration
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  val ws = services.ws
  val botResultService = services.botResultService
  implicit val actorSystem = services.actorSystem

  private def maybeResultFor[T](form: Form[T], resultFn: T => Result)
                               (implicit request: Request[AnyContent]): Option[Result] = {
    form.bindFromRequest.fold(
      _ => None,
      info => Some(resultFn(info))
    )
  }

  def add = silhouette.UserAwareAction { implicit request =>
    val maybeResult = for {
      scopes <- configuration.getOptional[String]("silhouette.slack.scope")
      clientId <- configuration.getOptional[String]("silhouette.slack.clientID")
    } yield {
        val redirectUrl = routes.SocialAuthController.installForSlack().absoluteURL(secure=true)
        Ok(views.html.slack.addToSlack(viewConfig(None), scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
  }

  def signIn(maybeRedirectUrl: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    val eventualMaybeTeamAccess = request.identity.map { user =>
      dataService.users.teamAccessFor(user, None).map(Some(_))
    }.getOrElse(Future.successful(None))
    eventualMaybeTeamAccess.map { maybeTeamAccess =>
      val maybeResult = for {
        scopes <- configuration.getOptional[String]("silhouette.slack.signInScope")
        clientId <- configuration.getOptional[String]("silhouette.slack.clientID")
      } yield {
          val redirectUrl = routes.SocialAuthController.authenticateSlack(maybeRedirectUrl).absoluteURL(secure=true)
          Ok(views.html.slack.signInWithSlack(viewConfig(maybeTeamAccess), scopes, clientId, UriEncoding.encodePathSegment(redirectUrl, "utf-8")))
        }
      maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
    }
  }

  trait RequestInfo {
    val token: String
    def isValid: Boolean = configuration.getOptional[String]("slack.token").contains(token)
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

  private def maybeChallengeResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(challengeRequestForm, (info: ChallengeRequestInfo) => {
      if (info.isValid) {
        Ok(info.challenge)
      } else {
        Forbidden("Bad token")
      }
    })
  }

  trait EventInfo {
    val eventType: String
  }

  case class AnyEventInfo(eventType: String) extends EventInfo

  trait EventRequestInfo {
    val teamId: String
    val maybeAuthedTeamIds: Option[Seq[String]]
    val event: EventInfo

    val teamIds: Seq[String] = maybeAuthedTeamIds.getOrElse(Seq(teamId))
  }

  case class ValidEventRequestInfo(
                                  token: String,
                                  teamId: String,
                                  maybeAuthedTeamIds: Option[Seq[String]],
                                  event: AnyEventInfo,
                                  requestType: String,
                                  eventId: String
                                ) extends EventRequestInfo with RequestInfo

  private val validEventRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "authed_teams" -> optional(seq(nonEmptyText)),
      "event" -> mapping(
        "type" -> nonEmptyText
      )(AnyEventInfo.apply)(AnyEventInfo.unapply),
      "type" -> nonEmptyText,
      "event_id" -> nonEmptyText
    )(ValidEventRequestInfo.apply)(ValidEventRequestInfo.unapply) verifying("Not a valid event request", fields => fields match {
      case info => info.requestType == "event_callback" && info.isValid
    })
  )

  def isValidEventRequest(implicit request: Request[AnyContent]): Boolean = {
    validEventRequestForm.bindFromRequest.fold(
      _ => false,
      _ => true
    )
  }

  trait MessageRequestInfo extends EventRequestInfo {
    val channel: String
    val userId: String
    val message: String
    val maybeThreadTs: Option[String]
    val ts: String
  }

  case class FileInfo(
                     createdAt: Long,
                     downloadUrl: String,
                     maybeThumbnailUrl: Option[String]
                     )

  case class MessageSentEventInfo(
                                   eventType: String,
                                   ts: String,
                                   maybeThreadTs: Option[String],
                                   userId: String,
                                   maybeSourceTeamId: Option[String],
                                   channel: String,
                                   text: String,
                                   maybeFileInfo: Option[FileInfo]
                                  ) extends EventInfo

  case class MessageSentRequestInfo(
                                          teamId: String,
                                          maybeAuthedTeamIds: Option[Seq[String]],
                                          event: MessageSentEventInfo
                                        ) extends MessageRequestInfo {
    val message: String = event.text.trim
    val userId: String = event.userId
    val channel: String = event.channel
    val ts: String = event.ts
    val maybeThreadTs: Option[String] = event.maybeThreadTs
  }
  private val messageSentRequestForm = Form(
    mapping(
      "team_id" -> nonEmptyText,
      "authed_teams" -> optional(seq(nonEmptyText)),
      "event" -> mapping(
        "type" -> nonEmptyText,
        "ts" -> nonEmptyText,
        "thread_ts" -> optional(nonEmptyText),
        "user" -> nonEmptyText,
        "source_team" -> optional(nonEmptyText),
        "channel" -> nonEmptyText,
        "text" -> nonEmptyText,
        "file" -> optional(mapping(
          "created" -> longNumber,
          "url_private_download" -> nonEmptyText,
          "thumb_1024" -> optional(nonEmptyText)
        )(FileInfo.apply)(FileInfo.unapply))
      )(MessageSentEventInfo.apply)(MessageSentEventInfo.unapply)
    )(MessageSentRequestInfo.apply)(MessageSentRequestInfo.unapply) verifying("Not a valid message event", fields => fields match {
      case info => info.event.eventType == "message"
    })
  )

  case class EditedInfo(user: String, ts: String)

  case class ChangedMessageInfo(
                                 eventType: String,
                                 ts: String,
                                 userId: String,
                                 maybeSourceTeamId: Option[String],
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
                                   ) extends EventInfo

  case class MessageChangedRequestInfo(
                                             teamId: String,
                                             maybeAuthedTeamIds: Option[Seq[String]],
                                             event: MessageChangedEventInfo
                                          ) extends MessageRequestInfo {
    val message: String = event.message.text.trim
    val userId: String = event.message.userId
    val channel: String = event.channel
    val ts: String = event.ts
    val maybeThreadTs: Option[String] = event.maybeThreadTs
  }

  private val messageChangedRequestForm = Form(
    mapping(
      "team_id" -> nonEmptyText,
      "authed_teams" -> optional(seq(nonEmptyText)),
      "event" -> mapping(
        "type" -> nonEmptyText,
        "message" -> mapping(
          "type" -> nonEmptyText,
          "ts" -> nonEmptyText,
          "user" -> nonEmptyText,
          "source_team" -> optional(nonEmptyText),
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
      )(MessageChangedEventInfo.apply)(MessageChangedEventInfo.unapply)
    )(MessageChangedRequestInfo.apply)(MessageChangedRequestInfo.unapply) verifying("Not an edited message event request", fields => fields match {
      case info => info.event.eventType == "message" && info.event.eventSubType == "message_changed"
    })
  )

  private def processEventsFor(info: MessageRequestInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    val maybeSourceTeamId = info.event match {
      case e: MessageChangedEventInfo => e.message.maybeSourceTeamId
      case e: MessageSentEventInfo => e.maybeSourceTeamId
      case _ => None
    }
    SlackMessage.fromFormattedText(info.message, botProfile, slackEventService).flatMap { slackMessage =>
      if (maybeSourceTeamId.exists(tid => botProfile.slackTeamId != tid && tid != LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)) {
        if (info.channel.startsWith("D") || botProfile.includesBotMention(slackMessage)) {
          sendEphemeralMessage("I only respond to my owners", info)
        } else {
          Future.successful({})
        }
      } else {
        val maybeFile = info.event match {
          case e: MessageSentEventInfo => e.maybeFileInfo.map(i => SlackFile(i.downloadUrl, i.maybeThumbnailUrl))
          case _ => None
        }
        for {
          _ <- slackEventService.onEvent(
            SlackMessageEvent(
              botProfile,
              info.channel,
              info.maybeThreadTs,
              info.userId,
              slackMessage,
              maybeFile,
              info.ts,
              slackEventService.clientFor(botProfile),
              None
            )
          )
        } yield {}
      }
    }
  }

  private def messageEventResult(info: MessageRequestInfo)(implicit request: Request[AnyContent]): Result = {
    println(Json.prettyPrint(request.body.asJson.getOrElse(Json.obj())))
    val isRetry = request.headers.get("X-Slack-Retry-Num").isDefined
    if (isRetry) {
      Ok("We are ignoring retries for now")
    } else {
      for {
        profiles <- Future.sequence(info.teamIds.map { teamId =>
          dataService.slackBotProfiles.allForSlackTeamId(teamId)
        }).map(_.flatten)
        _ <- Future.sequence(
          profiles.map { profile =>
            processEventsFor(info, profile)
          }
        )
      } yield {}

      // respond immediately
      Ok(":+1:")
    }
  }

  private def maybeMessageResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(messageSentRequestForm, messageEventResult) orElse
      maybeResultFor(messageChangedRequestForm, messageEventResult)
  }

  private def maybeEventResult(implicit request: Request[AnyContent]): Option[Result] = {
    if (isValidEventRequest) {
      maybeMessageResult
    } else {
      None
    }
  }

  def event = Action { implicit request =>
    (maybeChallengeResult orElse maybeEventResult).getOrElse {
      Ok("I don't know what to do with this request but I'm not concerned")
    }
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
  case class OriginalMessageInfo(
                                  text: String,
                                  attachments: Seq[AttachmentInfo],
                                  response_type: Option[String],
                                  replace_original: Option[Boolean],
                                  thread_ts: Option[String]
                                )
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
        _.value.map(HelpGroupSearchValue.fromString)
      }
    }

    def maybeActionListForSkillId: Option[HelpGroupSearchValue] = {
      actions.find(_.name == LIST_BEHAVIOR_GROUP_ACTIONS).flatMap {
        _.value.map(HelpGroupSearchValue.fromString)
      }
    }

    def maybeInputChoice: Option[String] = {
      val maybeSlackUserId = maybeUserIdForCallbackId(callback_id)
      maybeSlackUserId.flatMap { slackUserId =>
        if (user.id == slackUserId) {
          val maybeAction = actions.headOption
          val maybeValue = maybeAction.flatMap(_.value)
          maybeValue.orElse {
            for {
              selectedOptions <- maybeAction.map(_.selected_options)
              firstOption <- selectedOptions.map(_.headOption)
              response <- firstOption.map(_.value)
            } yield {
              response
            }
          }
        } else {
          None
        }
      }
    }

    def maybeIncorrectUserIdTryingInputChoice: Option[String] = {
      val maybeSlackUserId = maybeUserIdForCallbackId(callback_id)
      maybeSlackUserId.flatMap { slackUserId =>
        if (user.id != slackUserId) {
          Some(slackUserId)
        } else {
          None
        }
      }
    }

    def isForInputChoiceForDoneConversation: Future[Boolean] = {
      maybeConversationIdForCallbackId(callback_id).map { convoId =>
        dataService.conversations.find(convoId).map { maybeConvo =>
          maybeConvo.exists(_.isDone)
        }
      }.getOrElse(Future.successful(false))
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

    def maybeSelectedActionChoice: Option[ActionChoice] = {
      val maybeAction = actions.find(_.name == ACTION_CHOICE)
      maybeAction.flatMap(_.value).flatMap { value =>
        Try(Json.parse(value)) match {
          case Success(json) => json.asOpt[ActionChoice]
          case Failure(_) => None
        }
      }
    }

    def maybeYesNoAnswer: Option[String] = {
      actions.find(_.name == YES_NO_CHOICE).flatMap { action =>
        action.value.filter(v => v == YES || v == NO)
      }
    }

    private def originalMessageActions: Seq[ActionInfo] = {
      this.original_message.attachments.flatMap(_.actions).flatten
    }

    def findOptionLabelForValue(value: String): Option[String] = {
      for {
        select <- originalMessageActions.find(_.`type` == "select")
        options <- select.options
        matchingOption <- options.find(_.value == value)
        text <- matchingOption.text
      } yield {
        text
      }
    }

    def findButtonLabelForNameAndValue(name: String, value: String): Option[String] = {
      val maybeAction = originalMessageActions.find { action =>
        action.`type` == "button" && action.name == name && action.value.exists { actionValue =>
          actionValue == value || HelpGroupSearchValue.fromString(actionValue).helpGroupId == value
        }
      }
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

  private def sendEphemeralMessage(message: String, slackTeamId: String, slackChannelId: String, slackUserId: String): Future[Unit] = {
    for {
      maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(slackTeamId).map(_.headOption)
      _ <- (for {
        profile <- maybeProfile
      } yield {
        ws.
          url("https://slack.com/api/chat.postEphemeral").
          withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
          post(Map(
            "token" -> Seq(profile.token),
            "channel" -> Seq(slackChannelId),
            "text" -> Seq(message),
            "user" -> Seq(slackUserId)
          ))
      }).getOrElse {
        Future.successful({})
      }
    } yield {}
  }

  private def sendEphemeralMessage(message: String, info: ActionsTriggeredInfo): Future[Unit] = {
    sendEphemeralMessage(message, info.team.id, info.channel.id, info.user.id)
  }

  private def sendEphemeralMessage(message: String, info: MessageRequestInfo): Future[Unit] = {
    sendEphemeralMessage(message, info.teamId, info.channel, info.userId)
  }

  private def updateActionsMessageFor(
                                       info: ActionsTriggeredInfo,
                                       maybeResultText: Option[String],
                                       shouldRemoveActions: Boolean
                                     ): Future[Unit] = {
    val maybeOriginalColor = info.original_message.attachments.headOption.flatMap(_.color)
    val newAttachment = AttachmentInfo(maybeResultText, None, None, Some(Seq("text")), Some(info.callback_id), color = maybeOriginalColor, footer = maybeResultText)
    val originalAttachmentsToUse = if (shouldRemoveActions) {
      info.original_message.attachments.map(ea => ea.copy(actions = None))
    } else {
      info.original_message.attachments
    }
    val updated = info.original_message.copy(attachments = originalAttachmentsToUse :+ newAttachment)
    for {
      maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(info.team.id).map(_.headOption)
      _ <- (for {
        profile <- maybeProfile
      } yield {
        ws.
          url("https://slack.com/api/chat.update").
          withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
          post(Map(
            "token" -> Seq(profile.token),
            "channel" -> Seq(info.channel.id),
            "text" -> Seq(updated.text),
            "attachments" -> Seq(Json.prettyPrint(Json.toJson(updated.attachments))),
            "as_user" -> Seq("true"),
            "ts" -> Seq(info.message_ts),
            "user" -> Seq(info.user.id)
          ))
      }).getOrElse {
        Future.successful({})
      }
    } yield {}
  }

  private def inputChoiceResultFor(value: String, info: ActionsTriggeredInfo)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
      maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(info.team.id).map(_.headOption)
      maybeSlackMessage <- maybeProfile.map { profile =>
        SlackMessage.fromFormattedText(value, profile, slackEventService).map(Some(_))
      }.getOrElse(Future.successful(None))
      _ <- (for {
        profile <- maybeProfile
        slackMessage <- maybeSlackMessage
      } yield {
        slackEventService.onEvent(SlackMessageEvent(
          profile,
          info.channel.id,
          info.original_message.thread_ts,
          info.user.id,
          slackMessage,
          None,
          info.message_ts,
          slackEventService.clientFor(profile),
          None
        ))
      }).getOrElse {
        Future.successful({})
      }
    } yield {}
  }

  def action = Action { implicit request =>
    actionForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(formWithErrors.errorsAsJson)
      },
      payload => {

        // Slack improperly (?) displays escaped text inside button labels as-is in the client when
        // we return the original message back.
        //
        // TODO: Investigate whether this is safe and/or desirable
        val unescapedPayload = SlackMessage.unescapeSlackHTMLEntities(payload)

        Json.parse(unescapedPayload).validate[ActionsTriggeredInfo] match {
          case JsSuccess(info, jsPath) => {
            if (info.isValid) {
              var maybeResultText: Option[String] = None
              var shouldRemoveActions = false
              val slackUser = s"<@${info.user.id}>"

              info.maybeInputChoice.foreach { response =>
                info.isForInputChoiceForDoneConversation.flatMap { shouldStop =>
                  if (shouldStop) {
                    updateActionsMessageFor(info, Some(s"This conversation is no longer active"), shouldRemoveActions = true)
                  } else {
                    inputChoiceResultFor(response, info)
                  }
                }
                maybeResultText = Some(s"$slackUser chose $response")
                shouldRemoveActions = true
              }

              info.maybeIncorrectUserIdTryingInputChoice.foreach { correctUserId =>
                val correctUser = s"<@${correctUserId}>"
                sendEphemeralMessage(s"Only $correctUser can answer this", info)
              }

              info.maybeHelpIndexAt.foreach { index =>
                dataService.slackBotProfiles.sendResultWithNewEvent(
                  "help index",
                  (event) => DisplayHelpBehavior(
                    None,
                    None,
                    Some(index),
                    includeNameAndDescription = false,
                    includeNonMatchingResults = false,
                    isFirstTrigger = false,
                    event,
                    services
                  ).result.map(Some(_)),
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )
                maybeResultText = Some(s"$slackUser clicked More help.")
              }

              info.maybeHelpForSkillIdWithMaybeSearch.foreach { searchValue =>
                dataService.slackBotProfiles.sendResultWithNewEvent(
                  "skill help with maybe search",
                  (event) => DisplayHelpBehavior(
                    searchValue.maybeSearchText,
                    Some(searchValue.helpGroupId),
                    None,
                    includeNameAndDescription = true,
                    includeNonMatchingResults = false,
                    isFirstTrigger = false,
                    event,
                    services
                  ).result.map(Some(_)),
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )
                maybeResultText = Some(info.findButtonLabelForNameAndValue(SHOW_BEHAVIOR_GROUP_HELP, searchValue.helpGroupId).map { text =>
                  s"$slackUser clicked $text."
                } getOrElse {
                  s"$slackUser clicked a button."
                })
              }

              info.maybeActionListForSkillId.foreach { searchValue =>
                dataService.slackBotProfiles.sendResultWithNewEvent(
                  "for skill action list",
                  event => DisplayHelpBehavior(
                    searchValue.maybeSearchText,
                    Some(searchValue.helpGroupId),
                    None,
                    includeNameAndDescription = false,
                    includeNonMatchingResults = true,
                    isFirstTrigger = false,
                    event,
                    services
                  ).result.map(Some(_)),
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )
                maybeResultText = Some(s"$slackUser clicked List all actions")
              }

              info.maybeConfirmContinueConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.touch(convo).flatMap { _ =>
                      cacheService.getEvent(convo.pendingEventKey).map { event =>
                        slackEventService.onEvent(event)
                      }.getOrElse(Future.successful({}))
                    }
                  }.getOrElse(Future.successful({}))
                }
                shouldRemoveActions = true
                maybeResultText = Some(s"$slackUser clicked 'Yes'")
              }

              info.maybeDontContinueConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.background(convo, "OK, on to the next thing.", includeUsername = false).flatMap { _ =>
                      cacheService.getEvent(convo.pendingEventKey).map { event =>
                        eventHandler.handle(event, None).flatMap { results =>
                          Future.sequence(
                            results.map(result => services.botResultService.sendIn(result, None).map { _ =>
                              Logger.info(event.logTextFor(result, None))
                            })
                          )
                        }
                      }.getOrElse(Future.successful({}))
                    }
                  }.getOrElse(Future.successful({}))
                }
                shouldRemoveActions = true
                maybeResultText = Some(s"$slackUser clicked 'No'")
              }

              info.maybeStopConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.cancel(convo)
                  }.getOrElse(Future.successful({}))
                }
                shouldRemoveActions = true
                maybeResultText = Some(s"$slackUser stopped the conversation")
              }

              info.maybeYesNoAnswer.foreach { answer =>
                inputChoiceResultFor(answer, info)
                shouldRemoveActions = true
                maybeResultText = Some(s"$slackUser selected `${answer.capitalize}`")
              }

              info.maybeRunBehaviorVersionId.foreach { behaviorVersionId =>
                dataService.slackBotProfiles.sendResultWithNewEvent(
                  s"run behavior version $behaviorVersionId",
                  event => for {
                    maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId)
                    maybeResponse <- maybeBehaviorVersion.map { behaviorVersion =>
                      dataService.behaviorResponses.buildFor(
                        event,
                        behaviorVersion,
                        Map(),
                        None,
                        None
                      ).map(Some(_))
                    }.getOrElse(Future.successful(None))
                    maybeResult <- maybeResponse.map { response =>
                      response.result.map(Some(_))
                    }.getOrElse(Future.successful(None))
                  } yield maybeResult,
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )

                maybeResultText = Some(info.findButtonLabelForNameAndValue(RUN_BEHAVIOR_VERSION, behaviorVersionId).map { text =>
                  s"$slackUser clicked $text"
                } orElse info.findOptionLabelForValue(behaviorVersionId).map { text =>
                  s"$slackUser ran ${text.mkString("“", "", "”")}"
                } getOrElse {
                  s"$slackUser ran an action"
                })
              }

              info.maybeSelectedActionChoice.foreach { actionChoice =>
                dataService.slackBotProfiles.sendResultWithNewEvent(
                  s"run action named ${actionChoice.actionName}",
                  event => for {
                    maybeGroupVersion <- actionChoice.groupVersionId.map { groupVersionId =>
                      dataService.behaviorGroupVersions.findWithoutAccessCheck(groupVersionId)
                    }.getOrElse(Future.successful(None))
                    user <- event.ensureUser(dataService)
                    maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
                      dataService.behaviorGroupVersions.isActive(groupVersion, Conversation.SLACK_CONTEXT, info.channel.id).flatMap { isActive =>
                        if (isActive && actionChoice.canBeTriggeredBy(user)) {
                          dataService.behaviorVersions.findByName(actionChoice.actionName, groupVersion)
                        } else {
                          Future.successful(None)
                        }
                      }
                    }.getOrElse(Future.successful(None))
                    params <- maybeBehaviorVersion.map { behaviorVersion =>
                      dataService.behaviorParameters.allFor(behaviorVersion)
                    }.getOrElse(Future.successful(Seq()))
                    invocationParams <- Future.successful(actionChoice.argumentsMap.flatMap { case(name, value) =>
                      params.find(_.name == name).map { param =>
                        (AWSLambdaConstants.invocationParamFor(param.rank - 1), value)
                      }
                    })
                    maybeResponse <- maybeBehaviorVersion.map { behaviorVersion =>
                      dataService.behaviorResponses.buildFor(
                        event,
                        behaviorVersion,
                        invocationParams,
                        None,
                        None
                      ).map(Some(_))
                    }.getOrElse(Future.successful(None))
                    maybeResult <- maybeResponse.map { response =>
                      response.result.map(Some(_))
                    }.getOrElse(Future.successful(None))
                  } yield maybeResult,
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )

                dataService.runNow(for {
                  maybeGroupVersion <- actionChoice.groupVersionId.map { groupVersionId =>
                    dataService.behaviorGroupVersions.findWithoutAccessCheck(groupVersionId)
                  }.getOrElse(Future.successful(None))
                  isActive <- maybeGroupVersion.map { groupVersion =>
                    dataService.behaviorGroupVersions.isActive(groupVersion, Conversation.SLACK_CONTEXT, info.channel.id)
                  }.getOrElse(Future.successful(false))
                  maybeUser <- maybeGroupVersion.map { groupVersion =>
                    dataService.users.ensureUserFor(LoginInfo(Conversation.SLACK_CONTEXT, info.user.id), groupVersion.team.id).map(Some(_))
                  }.getOrElse(Future.successful(None))
                  maybeChoiceSlackUserId <- actionChoice.userId.map { userId =>
                    dataService.users.find(userId).flatMap { maybeUser =>
                      maybeUser.map { user =>
                        dataService.linkedAccounts.maybeSlackUserIdFor(user)
                      }.getOrElse(Future.successful(None))
                    }
                  }.getOrElse(Future.successful(None))
                } yield {
                  if (!isActive) {
                    shouldRemoveActions = true
                    maybeResultText = Some("This skill has been updated, making these associated actions no longer valid")
                  } else if (!maybeUser.exists(u => actionChoice.canBeTriggeredBy(u))) {
                    maybeResultText = Some(maybeChoiceSlackUserId.map { choiceSlackUserId =>
                      s"Only <@${choiceSlackUserId}> can make this choice"
                    }.getOrElse("You are not allowed to make this choice"))
                  } else {
                    shouldRemoveActions = true
                    maybeResultText = Some(s"$slackUser clicked ${actionChoice.label}")
                  }
                })
              }

              // respond immediately by appending a new attachment
              val maybeOriginalColor = info.original_message.attachments.headOption.flatMap(_.color)
              val newAttachment = AttachmentInfo(maybeResultText, None, None, Some(Seq("text")), Some(info.callback_id), color = maybeOriginalColor, footer = maybeResultText)
              val originalAttachmentsToUse = if (shouldRemoveActions) {
                info.original_message.attachments.map(ea => ea.copy(actions = None))
              } else {
                info.original_message.attachments
              }
              val updated = info.original_message.copy(attachments = originalAttachmentsToUse :+ newAttachment)
              Ok(Json.toJson(updated))
            } else {
              Forbidden("Bad token")
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
