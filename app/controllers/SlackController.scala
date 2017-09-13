package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events.{EventHandler, SlackMessage, SlackMessageEvent}
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import play.utils.UriEncoding
import services._

import scala.concurrent.{ExecutionContext, Future}

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
    val event: EventInfo
  }

  case class ValidEventRequestInfo(
                                  token: String,
                                  teamId: String,
                                  event: AnyEventInfo,
                                  requestType: String,
                                  eventId: String
                                ) extends EventRequestInfo with RequestInfo

  private val validEventRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "team_id" -> nonEmptyText,
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

  case class MessageSentEventInfo(
                                    eventType: String,
                                    ts: String,
                                    maybeThreadTs: Option[String],
                                    userId: String,
                                    channel: String,
                                    text: String
                                  ) extends EventInfo

  case class MessageSentRequestInfo(
                                          teamId: String,
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
      "event" -> mapping(
        "type" -> nonEmptyText,
        "ts" -> nonEmptyText,
        "thread_ts" -> optional(nonEmptyText),
        "user" -> nonEmptyText,
        "channel" -> nonEmptyText,
        "text" -> nonEmptyText
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
      )(MessageChangedEventInfo.apply)(MessageChangedEventInfo.unapply)
    )(MessageChangedRequestInfo.apply)(MessageChangedRequestInfo.unapply) verifying("Not an edited message event request", fields => fields match {
      case info => info.event.eventType == "message" && info.event.eventSubType == "message_changed"
    })
  )

  private def messageEventResult(info: MessageRequestInfo)(implicit request: Request[AnyContent]): Result = {
    val isRetry = request.headers.get("X-Slack-Retry-Num").isDefined
    if (isRetry) {
      Ok("We are ignoring retries for now")
    } else {
      for {
        maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(info.teamId).map(_.headOption)
        maybeSlackMessage <- maybeProfile.map { profile =>
          SlackMessage.fromFormattedText(info.message, profile, slackEventService).map(Some(_))
        }.getOrElse(Future.successful(None))
        _ <- (for {
          profile <- maybeProfile
          slackMessage <- maybeSlackMessage
        } yield {
          slackEventService.onEvent(SlackMessageEvent(
            profile,
            info.channel,
            info.maybeThreadTs,
            info.userId,
            slackMessage,
            info.ts,
            slackEventService.clientFor(profile)
          ))
        }).getOrElse {
          Future.successful({})
        }
      } yield {}

      // respond immediately
      Ok(":+1:")
    }
  }

  private def maybeMessageResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(messageSentRequestForm, messageEventResult) orElse
      maybeResultFor(messageChangedRequestForm, messageEventResult)
  }

  case class ChannelMembersChangedEventInfo(
                                          eventType: String,
                                          user: String,
                                          channel: String,
                                          channelType: String
                                         ) extends EventInfo

  case class ChannelMembersChangedRequestInfo(
                                               teamId: String,
                                               event: ChannelMembersChangedEventInfo
                                             ) extends EventRequestInfo

  private val channelMembersChangedPattern = "(member_joined_channel|member_left_channel)"

  private val channelMembersChangedRequestForm = Form(
    mapping(
      "team_id" -> nonEmptyText,
      "event" -> mapping(
        "type" -> nonEmptyText,
        "user" -> nonEmptyText,
        "channel" -> nonEmptyText,
        "channel_type" -> nonEmptyText
      )(ChannelMembersChangedEventInfo.apply)(ChannelMembersChangedEventInfo.unapply)
    )(ChannelMembersChangedRequestInfo.apply)(ChannelMembersChangedRequestInfo.unapply) verifying("Not a valid channel event", fields => fields match {
      case info => info.event.eventType.matches(channelMembersChangedPattern)
    })
  )

  private def updateMembers(eventType: String, oldMembers: Seq[String], member: String): Seq[String] = {
    if (eventType == "member_joined_channel") {
      oldMembers ++ Seq(member)
    } else {
      oldMembers.filterNot(_ == member)
    }
  }

  private def maybeChannelMembersChangedResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(channelMembersChangedRequestForm, (info: ChannelMembersChangedRequestInfo) => {
      val channelId = info.event.channel
      val channelType = info.event.channelType
      val teamId = info.teamId
      val eventType = info.event.eventType
      val userId = info.event.user
      if (channelType == "C") {
        val maybeOldChannel = services.cacheService.getSlackChannelInfo(channelId, teamId)
        services.cacheService.uncacheSlackChannelInfo(channelId, teamId)
        maybeOldChannel.foreach { oldChannel =>
          val newChannel = oldChannel.copy(
            members = oldChannel.members.map((members) => updateMembers(eventType, members, userId))
          )
          services.cacheService.cacheSlackChannelInfo(channelId, teamId, newChannel)
        }
      } else if (channelType == "G") {
        val maybeOldGroup = services.cacheService.getSlackGroupInfo(channelId, teamId)
        services.cacheService.uncacheSlackGroupInfo(channelId, teamId)
        maybeOldGroup.foreach { oldGroup =>
          val newGroup = oldGroup.copy(
            members = updateMembers(eventType, oldGroup.members, userId)
          )
          services.cacheService.cacheSlackGroupInfo(channelId, teamId, newGroup)
        }
      }
      Ok(":+1:")
    })
  }

  case class UserChangeInfo(id: String)
  case class UserProfileChangedEventInfo(
                                          eventType: String,
                                          user: UserChangeInfo
                                        ) extends EventInfo
  case class UserProfileChangedRequestInfo(
                                             teamId: String,
                                             event: UserProfileChangedEventInfo
                                           ) extends EventRequestInfo

  private val userProfileChangedRequestForm = Form(
    mapping(
      "team_id" -> nonEmptyText,
      "event" -> mapping(
        "type" -> nonEmptyText,
        "user" -> mapping(
          "id" -> nonEmptyText
        )(UserChangeInfo.apply)(UserChangeInfo.unapply)
      )(UserProfileChangedEventInfo.apply)(UserProfileChangedEventInfo.unapply)
    )(UserProfileChangedRequestInfo.apply)(UserProfileChangedRequestInfo.unapply) verifying("Not a valid user event", fields => fields match {
      case info => info.event.eventType == "user_change"
    })
  )

  private def maybeUserProfileChangedResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(userProfileChangedRequestForm, (info: UserProfileChangedRequestInfo) => {
      val slackUserId = info.event.user.id
      val slackTeamId = info.teamId
      services.cacheService.uncacheSlackUserData(slackUserId, slackTeamId)
      Ok(":+1:")
    })
  }


  private def maybeEventResult(implicit request: Request[AnyContent]): Option[Result] = {
    if (isValidEventRequest) {
      maybeMessageResult orElse
        maybeChannelMembersChangedResult orElse
        maybeUserProfileChangedResult
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
        _.value.map(HelpGroupSearchValue.fromString)
      }
    }

    def maybeActionListForSkillId: Option[HelpGroupSearchValue] = {
      actions.find(_.name == LIST_BEHAVIOR_GROUP_ACTIONS).flatMap {
        _.value.map(HelpGroupSearchValue.fromString)
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
        val unescapedPayload = SlackMessage.unescapeSlackHTMLEntities(payload)

        Json.parse(unescapedPayload).validate[ActionsTriggeredInfo] match {
          case JsSuccess(info, jsPath) => {
            if (info.isValid) {
              var resultText: String = "OK, let’s continue."
              var shouldRemoveActions = false
              val user = s"<@${info.user.id}>"

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
                    lambdaService,
                    dataService,
                    cacheService
                  ).result.map(Some(_)),
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )
                resultText = s"$user clicked More help."
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
                    lambdaService,
                    dataService,
                    cacheService
                  ).result.map(Some(_)),
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )
                resultText = info.findButtonLabelForNameAndValue(SHOW_BEHAVIOR_GROUP_HELP, searchValue.helpGroupId).map { text =>
                  s"$user clicked $text."
                } getOrElse {
                  s"$user clicked a button."
                }
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
                    lambdaService,
                    dataService,
                    cacheService
                  ).result.map(Some(_)),
                  info.team.id,
                  info.channel.id,
                  info.user.id,
                  info.message_ts
                )
                resultText = s"$user clicked List all actions"
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
                resultText = s"$user clicked 'Yes'"
              }

              info.maybeDontContinueConversationId.foreach { conversationId =>
                dataService.conversations.find(conversationId).flatMap { maybeConversation =>
                  maybeConversation.map { convo =>
                    dataService.conversations.background(convo, "OK, on to the next thing.", includeUsername = false).flatMap { _ =>
                      cacheService.getEvent(convo.pendingEventKey).map { event =>
                        eventHandler.handle(event, None).flatMap { results =>
                          Future.sequence(
                            results.map(result => services.botResultService.sendIn(result, None).map { _ =>
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
