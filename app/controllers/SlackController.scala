package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import javax.inject.Inject
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{Normal, Threaded}
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.events._
import models.behaviors.{ActionChoice, SimpleTextResult}
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Environment, Logger, Mode}
import play.utils.UriEncoding
import services._
import services.slack.SlackEventService
import utils.SlashCommandInfo

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SlackController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val eventHandler: EventHandler,
                                  val slackEventService: SlackEventService,
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
    val maybeEnterpriseId: Option[String]
    val teamId: String
    val maybeAuthedTeamIds: Option[Seq[String]]
    val event: EventInfo

    def slackTeamIdsForBots: Seq[String] = {
      if (maybeEnterpriseId.isDefined) {
        (for {
          authedTeamIds <- maybeAuthedTeamIds
          authedTeamId <- authedTeamIds.find(_ == teamId).orElse(authedTeamIds.headOption)
        } yield {
          Seq(authedTeamId)
        }).getOrElse(Seq(teamId))
      } else {
        maybeAuthedTeamIds.getOrElse(Seq(teamId))
      }
    }

    def isEnterpriseGrid: Boolean = maybeEnterpriseId.nonEmpty
  }

  case class ValidEventRequestInfo(
                                    token: String,
                                    maybeEnterpriseId: Option[String],
                                    teamId: String,
                                    maybeAuthedTeamIds: Option[Seq[String]],
                                    event: AnyEventInfo,
                                    requestType: String,
                                    eventId: String
                                ) extends EventRequestInfo with RequestInfo

  private val validEventRequestForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "enterprise_id" -> optional(nonEmptyText),
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

  trait ItemInfo {
    val itemType: String
    val channel: String
  }

  case class MessageItemInfo(
                            itemType: String,
                            channel: String,
                            ts: String
                            ) extends ItemInfo

  case class ReactionAddedEventInfo(
                                   eventType: String,
                                   userId: String,
                                   reaction: String,
                                   itemUserId: Option[String],
                                   item: MessageItemInfo,
                                   eventTs: String
                                   ) extends EventInfo

  case class ReactionAddedRequestInfo(
                                     maybeEnterpriseId: Option[String],
                                     teamId: String,
                                     maybeAuthedTeamIds: Option[Seq[String]],
                                     event: ReactionAddedEventInfo
                                   ) extends EventRequestInfo {
    val userId: String = event.userId
    val channel: String = event.item.channel
    val ts: String = event.eventTs
  }
  private val reactionAddedRequestForm = Form(
    mapping(
      "enterprise_id" -> optional(nonEmptyText),
      "team_id" -> nonEmptyText,
      "authed_teams" -> optional(seq(nonEmptyText)),
      "event" -> mapping(
        "type" -> nonEmptyText,
        "user" -> nonEmptyText,
        "reaction" -> nonEmptyText,
        "item_user" -> optional(nonEmptyText),
        "item" -> mapping(
          "type" -> nonEmptyText,
          "channel" -> nonEmptyText,
          "ts" -> nonEmptyText
        )(MessageItemInfo.apply)(MessageItemInfo.unapply),
        "event_ts" -> nonEmptyText
        )(ReactionAddedEventInfo.apply)(ReactionAddedEventInfo.unapply)
      )(ReactionAddedRequestInfo.apply)(ReactionAddedRequestInfo.unapply) verifying("Not a valid message event", fields => fields match {
      case info => info.event.eventType == "reaction_added"
    })
  )


  case class MessageSentEventInfo(
                                   eventType: String,
                                   ts: String,
                                   maybeThreadTs: Option[String],
                                   userId: String,
                                   maybeSourceTeamId: Option[String],
                                   channel: String,
                                   text: String,
                                   maybeFilesInfo: Option[Seq[FileInfo]]
                                  ) extends EventInfo

  case class MessageSentRequestInfo(
                                     maybeEnterpriseId: Option[String],
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
      "enterprise_id" -> optional(nonEmptyText),
      "team_id" -> nonEmptyText,
      "authed_teams" -> optional(seq(nonEmptyText)),
      "event" -> mapping(
        "type" -> nonEmptyText,
        "ts" -> nonEmptyText,
        "thread_ts" -> optional(nonEmptyText),
        "user" -> nonEmptyText,
        "team" -> optional(nonEmptyText),
        "channel" -> nonEmptyText,
        "text" -> text,
        "files" -> optional(seq(mapping(
          "created" -> longNumber,
          "url_private_download" -> nonEmptyText,
          "thumb_1024" -> optional(nonEmptyText)
        )(FileInfo.apply)(FileInfo.unapply)))
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
                                        maybeEnterpriseId: Option[String],
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
      "enterprise_id" -> optional(nonEmptyText),
      "team_id" -> nonEmptyText,
      "authed_teams" -> optional(seq(nonEmptyText)),
      "event" -> mapping(
        "type" -> nonEmptyText,
        "message" -> mapping(
          "type" -> nonEmptyText,
          "ts" -> nonEmptyText,
          "user" -> nonEmptyText,
          "team" -> optional(nonEmptyText),
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

  private def processReactionEventsFor(info: ReactionAddedRequestInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
      maybeSlackMessage <- SlackMessage.maybeFromMessageTs(info.event.item.ts, info.event.item.channel, botProfile, services)
      isUserValidForBot <- slackEventService.isUserValidForBot(info.userId, botProfile, info.maybeEnterpriseId)
      result <- if (!isUserValidForBot) {
        Future.successful({})
      } else {
        val event = SlackReactionAddedEvent(
          SlackEventContext(
            botProfile,
            info.channel,
            maybeThreadId = None,
            info.userId
          ),
          info.event.reaction,
          maybeSlackMessage
        )
        slackEventService.onEvent(event)
      }
    } yield result
  }

  private def processMessageEventsFor(info: MessageRequestInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
      slackMessage <- SlackMessage.fromFormattedText(info.message, botProfile, slackEventService)
      isUserValidForBot <- slackEventService.isUserValidForBot(info.userId, botProfile, info.maybeEnterpriseId)
      result <- if (!isUserValidForBot) {
        if (info.channel.startsWith("D") || botProfile.includesBotMention(slackMessage)) {
          dataService.teams.find(botProfile.teamId).flatMap { maybeTeam =>
            val teamText = maybeTeam.map { team =>
              s"the ${team.name} team"
            }.getOrElse("another team")
            sendEphemeralMessage(s"Sorry, I'm only able to respond to people from ${teamText}.", info)
          }
        } else {
          Future.successful({})
        }
      } else {
        val maybeFile = info.event match {
          case e: MessageSentEventInfo => e.maybeFilesInfo.flatMap(_.headOption.map(i => SlackFile(i.downloadUrl, i.maybeThumbnailUrl)))
          case _ => None
        }
        slackEventService.onEvent(
          SlackMessageEvent(
            SlackEventContext(
              botProfile,
              info.channel,
              info.maybeThreadTs,
              info.userId
            ),
            slackMessage,
            maybeFile,
            info.ts,
            None,
            isUninterruptedConversation = false,
            isEphemeral = false,
            None,
            beQuiet = false
          )
        )
      }
    } yield result
  }

  private def reactionAddedEventResult(info: ReactionAddedRequestInfo)(implicit request: Request[AnyContent]): Result = {
    val isRetry = request.headers.get("X-Slack-Retry-Num").isDefined
    if (isRetry) {
      Ok("We are ignoring retries for now")
    } else {
      for {
        profiles <- Future.sequence(info.slackTeamIdsForBots.map { teamId =>
          dataService.slackBotProfiles.allForSlackTeamId(teamId)
        }).map(_.flatten)
        _ <- Future.sequence(
          profiles.map { profile =>
            if (profile.userId != info.userId) {
              processReactionEventsFor(info, profile)
            } else {
              Future.successful({})
            }
          }
        )
      } yield {}

      // respond immediately
      Ok(":+1:")
    }
  }

  private def messageEventResult(info: MessageRequestInfo)(implicit request: Request[AnyContent]): Result = {
    val isRetry = request.headers.get("X-Slack-Retry-Num").isDefined
    if (isRetry) {
      Ok("We are ignoring retries for now")
    } else {
      for {
        profiles <- Future.sequence(info.slackTeamIdsForBots.map(dataService.slackBotProfiles.allForSlackTeamId)).map(_.flatten)
        _ <- Future.sequence(
          profiles.map { profile =>
            processMessageEventsFor(info, profile)
          }
        )
      } yield {}

      // respond immediately
      Ok(":+1:")
    }
  }

  private def maybeReactionResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(reactionAddedRequestForm, reactionAddedEventResult)
  }

  private def maybeMessageResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(messageSentRequestForm, messageEventResult) orElse
      maybeResultFor(messageChangedRequestForm, messageEventResult)
  }

  private def maybeEventResult(implicit request: Request[AnyContent]): Option[Result] = {
    if (isValidEventRequest) {
      maybeMessageResult.orElse(maybeReactionResult)
    } else {
      None
    }
  }

  def event = Action { implicit request =>
    if (environment.mode == Mode.Dev) {
      Logger.info(s"Slack event received:\n${Json.prettyPrint(request.body.asJson.get)}")
    }
    (maybeChallengeResult orElse maybeEventResult).getOrElse {
      Ok("I don't know what to do with this request but I'm not concerned")
    }
  }

  private val slashCommandForm = Form(
    mapping(
      "command" -> nonEmptyText,
      "text" -> text,
      "response_url" -> nonEmptyText,
      "user_id" -> nonEmptyText,
      "team_id" -> nonEmptyText,
      "channel_id" -> nonEmptyText
    )(SlashCommandInfo.apply)(SlashCommandInfo.unapply)
  )

  private def processCommandFor(info: SlashCommandInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
      slackMessage <- SlackMessage.fromFormattedText(info.text, botProfile, slackEventService)
      event <- Future.successful(SlashCommandEvent(
        SlackEventContext(
          botProfile,
          info.channelId,
          maybeThreadId = None,
          info.userId
        ),
        slackMessage,
        info.responseUrl
      ))
      maybeConversation <- dataService.conversations.allOngoingFor(event.eventContext, None).map(_.headOption)
      results <- eventHandler.handle(event, maybeConversation)
      _ <- Future.sequence(
        results.map(result => botResultService.sendIn(result, None).map { _ =>
          Logger.info(event.logTextFor(result, None))
        })
      )
    } yield {}
  }


  private def slashCommandResult(info: SlashCommandInfo)(implicit request: Request[AnyContent]): Result = {
    for {
      profiles <- dataService.slackBotProfiles.allForSlackTeamId(info.teamId)
      _ <- Future.sequence(
        profiles.map { profile =>
          processCommandFor(info, profile)
        }
      )
    } yield {}

    // respond immediately
    Ok(info.confirmation)
  }

  def command = Action { implicit request =>
    maybeResultFor(slashCommandForm, slashCommandResult).getOrElse {
      Ok("I don't know what to do with this request but I'm not concerned")
    }
  }

  case class ActionSelectOptionInfo(text: Option[String], value: String)
  case class ActionTriggeredInfo(name: String, value: Option[String], selected_options: Option[Seq[ActionSelectOptionInfo]]) {
    val maybeValue: Option[String] = {
      value.flatMap { v =>
        try {
          cacheService.getSlackActionValue(v)
        } catch {
          case e: IllegalArgumentException => None
        }
      }.orElse(value)
    }
  }
  case class ActionInfo(
                         name: String,
                         text: String,
                         value: Option[String],
                         `type`: String,
                         style: Option[String],
                         options: Option[Seq[ActionSelectOptionInfo]],
                         selected_options: Option[Seq[ActionSelectOptionInfo]]
                       )
  case class TeamInfo(id: String, enterprise_id: Option[String], domain: String) {
    val isEnterpriseGrid: Boolean = enterprise_id.isDefined
  }
  case class ChannelInfo(id: String, name: String) {
    val isDirectMessage: Boolean = id.startsWith("D")
  }
  case class UserInfo(id: String, name: String, team_id: Option[String])
  case class OriginalMessageInfo(
                                  text: String,
                                  attachments: Seq[AttachmentInfo],
                                  ts: String,
                                  response_type: Option[String],
                                  replace_original: Option[Boolean],
                                  thread_ts: Option[String],
                                  source_team: Option[String]
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
                                   original_message: Option[OriginalMessageInfo],
                                   response_url: String
                                 ) extends RequestInfo {

    val maybeOriginalMessageThreadId: Option[String] = {
      val maybeThreadId = original_message.flatMap(_.thread_ts)
      val maybeOriginalMessageId = original_message.map(_.ts)
      if (maybeThreadId != maybeOriginalMessageId) {
        maybeThreadId
      } else {
        None
      }
    }
    val isEphemeral: Boolean = original_message.isEmpty

    def slackTeamIdForUser: String = user.team_id.getOrElse(team.id)
    def slackTeamIdForBot: String = team.id

    def maybeBotProfile: Future[Option[SlackBotProfile]] = {
      dataService.slackBotProfiles.allForSlackTeamId(slackTeamIdForBot).map(_.headOption)
    }

    def maybeHelpForSkillIdWithMaybeSearch: Option[HelpGroupSearchValue] = {
      actions.find(_.name == SHOW_BEHAVIOR_GROUP_HELP).flatMap {
        _.maybeValue.map(HelpGroupSearchValue.fromString)
      }
    }

    def maybeActionListForSkillId: Option[HelpGroupSearchValue] = {
      actions.find(_.name == LIST_BEHAVIOR_GROUP_ACTIONS).flatMap {
        _.maybeValue.map(HelpGroupSearchValue.fromString)
      }
    }

    def maybeDataTypeChoice: Option[String] = {
      val maybeSlackUserId = maybeUserIdForCallbackId(DATA_TYPE_CHOICE, callback_id)
      maybeSlackUserId.flatMap { slackUserId =>
        if (user.id == slackUserId) {
          val maybeAction = actions.headOption
          val maybeValue = maybeAction.flatMap(_.maybeValue)
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

    val maybeUserIdForDataTypeChoice: Option[String] = maybeUserIdForCallbackId(DATA_TYPE_CHOICE, callback_id)

    def isIncorrectUserTryingDataTypeChoice: Boolean = {
      maybeUserIdForDataTypeChoice.exists { correctUserId =>
        user.id != correctUserId
      }
    }

    def isForDataTypeChoiceForDoneConversation: Future[Boolean] = {
      maybeConversationIdForCallbackId(DATA_TYPE_CHOICE, callback_id).map { convoId =>
        dataService.conversations.find(convoId).map { maybeConvo =>
          maybeConvo.exists(_.isDone)
        }
      }.getOrElse(Future.successful(false))
    }

    def maybeHelpIndexAt: Option[Int] = {
      actions.find { info => info.name == SHOW_HELP_INDEX }.map { _.maybeValue.map { value =>
        try {
          value.toInt
        } catch {
          case _: NumberFormatException => 0
        }
      }.getOrElse(0) }
    }

    val maybeConfirmContinueConversationId: Option[String] = maybeConversationIdForCallbackId(CONFIRM_CONTINUE_CONVERSATION, callback_id)

    val maybeConfirmContinueConversationUserId: Option[String] = maybeUserIdForCallbackId(CONFIRM_CONTINUE_CONVERSATION, callback_id)

    def maybeConfirmContinueConversationAnswer: Option[Boolean] = {
      actions.find(_.name == callback_id).flatMap { action =>
        action.maybeValue.filter(v => v == YES || v == NO).map(_ == YES)
      }
    }

    val maybeConfirmContinueConversationResponse: Option[ConfirmContinueConversationResponse] = {
      for {
        conversationId <- maybeConfirmContinueConversationId
        userId <- maybeConfirmContinueConversationUserId
        value <- maybeConfirmContinueConversationAnswer
      } yield ConfirmContinueConversationResponse(value, conversationId, userId)
    }

    val maybeStopConversationId: Option[String] = maybeConversationIdForCallbackId(STOP_CONVERSATION, callback_id)

    val maybeStopConversationUserId: Option[String] = maybeUserIdForCallbackId(STOP_CONVERSATION, callback_id)

    val maybeStopConversationResponse: Option[StopConversationResponse] = {
      for {
        conversationId <- maybeStopConversationId
        userId <- maybeStopConversationUserId
      } yield StopConversationResponse(conversationId, userId)
    }

    def maybeHelpRunBehaviorVersionId: Option[String] = {
      val maybeAction = actions.find(_.name == BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION)
      val maybeValue = maybeAction.flatMap(_.maybeValue)
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
      maybeAction.flatMap(_.maybeValue).flatMap { value =>
        Try(Json.parse(value)) match {
          case Success(json) => json.asOpt[ActionChoice]
          case Failure(_) => None
        }
      }
    }

    def maybeYesNoAnswer: Option[String] = {
      val maybeSlackUserId = maybeUserIdForCallbackId(YES_NO_CHOICE, callback_id)
      maybeSlackUserId.flatMap { slackUserId =>
        if (user.id == slackUserId) {
          actions.find(_.name == callback_id).flatMap { action =>
            action.maybeValue.filter(v => v == YES || v == NO)
          }
        } else {
          None
        }
      }
    }

    val maybeUserIdForYesNoChoice: Option[String] = maybeUserIdForCallbackId(YES_NO_CHOICE, callback_id)

    def isIncorrectUserTryingYesNo: Boolean = {
      maybeUserIdForYesNoChoice.exists { correctId =>
        user.id != correctId
      }
    }

    def isForYesNoForDoneConversation: Future[Boolean] = {
      maybeConversationIdForCallbackId(YES_NO_CHOICE, callback_id).map { convoId =>
        dataService.conversations.find(convoId).map { maybeConvo =>
          maybeConvo.exists(_.isDone)
        }
      }.getOrElse(Future.successful(false))
    }

    private def originalMessageActions: Seq[ActionInfo] = {
      this.original_message.map { msg =>
        msg.attachments.flatMap(_.actions).flatten
      }.getOrElse(Seq())
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

  case class ConfirmContinueConversationResponse(shouldContinue: Boolean, conversationId: String, userId: String)

  case class StopConversationResponse(conversationId: String, userId: String)

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

  private def sendEphemeralMessage(message: String, slackTeamId: String, slackChannelId: String, maybeThreadTs: Option[String], slackUserId: String): Future[Unit] = {
    for {
      maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(slackTeamId).map(_.headOption)
      _ <- (for {
        profile <- maybeProfile
      } yield {
        services.slackApiService.clientFor(profile).postEphemeralMessage(message, slackChannelId, maybeThreadTs, slackUserId)
      }).getOrElse {
        Future.successful({})
      }
    } yield {}
  }

  private def sendEphemeralMessage(message: String, info: ActionsTriggeredInfo): Future[Unit] = {
    sendEphemeralMessage(message, info.slackTeamIdForBot, info.channel.id, info.maybeOriginalMessageThreadId, info.user.id)
  }

  private def sendEphemeralMessage(message: String, info: MessageRequestInfo): Future[Unit] = {
    info.slackTeamIdsForBots.headOption.map { slackTeamId =>
      sendEphemeralMessage(message, slackTeamId, info.channel, info.maybeThreadTs, info.userId)
    }.getOrElse(Future.successful({}))
  }

  private def updateActionsMessageFor(
                                       info: ActionsTriggeredInfo,
                                       maybeResultText: Option[String],
                                       shouldRemoveActions: Boolean
                                     ): Future[Unit] = {
    info.original_message.map { originalMessage =>
      val maybeOriginalColor = originalMessage.attachments.headOption.flatMap(_.color)
      val newAttachment = AttachmentInfo(maybeResultText, None, None, Some(Seq("text")), Some(info.callback_id), color = maybeOriginalColor, footer = maybeResultText)
      val originalAttachmentsToUse = if (shouldRemoveActions) {
        originalMessage.attachments.map(ea => ea.copy(actions = None))
      } else {
        originalMessage.attachments
      }
      val updated = originalMessage.copy(attachments = originalAttachmentsToUse :+ newAttachment)
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
    }.getOrElse {
      Future.successful({})
    }
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
          SlackEventContext(
            profile,
            info.channel.id,
            info.maybeOriginalMessageThreadId,
            info.user.id
          ),
          slackMessage,
          None,
          info.message_ts,
          None,
          isUninterruptedConversation = false,
          info.isEphemeral,
          Some(info.response_url),
          beQuiet = false
        ))
      }).getOrElse {
        Future.successful({})
      }
    } yield {}
  }

  private def maybeSlackUserIdForActionChoice(actionChoice: ActionChoice): Future[Option[String]] = {
    dataService.users.find(actionChoice.userId).flatMap { maybeUser =>
      maybeUser.map { user =>
        dataService.linkedAccounts.maybeSlackUserIdFor(user)
      }.getOrElse(Future.successful(None))
    }
  }

  private def cannotBeTriggeredMessageFor(
                                           actionChoice: ActionChoice,
                                           maybeGroupVersion: Option[BehaviorGroupVersion],
                                           maybeChoiceSlackUserId: Option[String]
                                         ): String = {
    if (actionChoice.areOthersAllowed) {
      val teamText = maybeGroupVersion.map { bgv => s" ${bgv.team.name}"}.getOrElse("")
      s"Only members of the${teamText} team can make this choice"
    } else {
      maybeChoiceSlackUserId.map { choiceSlackUserId =>
        s"Only <@${choiceSlackUserId}> can make this choice"
      }.getOrElse("You are not allowed to make this choice")
    }
  }

  private def sendCannotBeTriggeredFor(
                                        actionChoice: ActionChoice,
                                        maybeGroupVersion: Option[BehaviorGroupVersion],
                                        info: ActionsTriggeredInfo
                                      ): Future[Unit] = {
    for {
      maybeChoiceSlackUserId <- maybeSlackUserIdForActionChoice(actionChoice)
      _ <- {
        val msg = cannotBeTriggeredMessageFor(actionChoice, maybeGroupVersion, maybeChoiceSlackUserId)
        sendEphemeralMessage(msg, info)
      }
    } yield {}
  }

  private def processTriggerableAndActiveActionChoice(
                                                       actionChoice: ActionChoice,
                                                       maybeGroupVersion: Option[BehaviorGroupVersion],
                                                       info: ActionsTriggeredInfo,
                                                       botProfile: SlackBotProfile,
                                                       maybeInstantResponseTs: Option[String]
                                                     ): Future[Unit] = {
    for {
      maybeThreadIdToUse <- info.maybeOriginalMessageThreadId.map(tid => Future.successful(Some(tid))).getOrElse {
        dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId).map { maybeOriginatingBehaviorVersion =>
          if (maybeOriginatingBehaviorVersion.exists(_.responseType == Threaded)) {
            maybeInstantResponseTs.orElse(info.original_message.map(_.ts))
          } else {
            None
          }
        }
      }
      _ <- dataService.slackBotProfiles.sendResultWithNewEvent(
        s"run action named ${actionChoice.actionName}",
        event => for {
          maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
            dataService.behaviorVersions.findByName(actionChoice.actionName, groupVersion)
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
              None,
              None,
              userExpectsResponse = true
            ).map(Some(_))
          }.getOrElse(Future.successful(None))
          maybeResult <- maybeResponse.map { response =>
            response.result.map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield maybeResult,
        botProfile,
        info.channel.id,
        info.user.id,
        info.message_ts,
        maybeThreadIdToUse,
        info.isEphemeral,
        Some(info.response_url),
        actionChoice.shouldBeQuiet
      )
    } yield {}
  }

  trait ActionPermission {

    val info: ActionsTriggeredInfo
    val shouldRemoveActions: Boolean
    val maybeResultText: Option[String]
    val beQuiet: Boolean = false
    def slackUser: String = if (info.channel.isDirectMessage || beQuiet) {
      "You"
    } else {
      s"<@${info.user.id}>"
    }
    implicit val request: Request[AnyContent]

    def instantBackgroundResponse(responseText: String): Future[Option[String]] = {
      val trimmed = responseText.trim.replaceAll("(^\\u00A0|\\u00A0$)", "")
      if (trimmed.isEmpty) {
        Future.successful(None)
      } else {
        for {
          maybeBotProfile <- info.maybeBotProfile
          maybeTs <- maybeBotProfile.map { botProfile =>
            dataService.slackBotProfiles.sendResultWithNewEvent(
              "Message acknowledging response to Slack action",
              slackMessageEvent => for {
                maybeConversation <- slackMessageEvent.maybeOngoingConversation(dataService)
              } yield {
                Some(SimpleTextResult(
                  slackMessageEvent,
                  maybeConversation,
                  s"_${trimmed}_",
                  responseType = Normal,
                  shouldInterrupt = false
                ))
              },
              botProfile,
              info.channel.id,
              info.user.id,
              info.message_ts,
              info.maybeOriginalMessageThreadId,
              info.isEphemeral || beQuiet,
              Some(info.response_url),
              beQuiet = false
            )
          }.getOrElse(Future.successful(None))
        } yield maybeTs
      }
    }

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit

    def result: Result = {

      // respond immediately by sending a new message
      val instantResponse = maybeResultText.map(instantBackgroundResponse).getOrElse(Future.successful(None))
      runInBackground(instantResponse)

      val updated = if (shouldRemoveActions) {
        info.original_message.map { originalMessage =>
          val maybeOriginalColor = originalMessage.attachments.headOption.flatMap(_.color)
          val newAttachment = AttachmentInfo(
            maybeResultText,
            title = None,
            text = None,
            Some(Seq("text")),
            Some(info.callback_id),
            color = maybeOriginalColor,
            footer = Some("✔︎︎ OK")
          )
          val attachments = originalMessage.attachments.map(ea => ea.copy(actions = None))
          originalMessage.copy(attachments = attachments :+ newAttachment)
        }
      } else {
        info.original_message
      }
      updated.map { u =>
        Ok(Json.toJson(u))
      }.getOrElse(Ok(""))
    }

  }

  trait ActionPermissionType[T <: ActionPermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[T]]

    def maybeResultFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[Result]] = {
      maybeFor(info, botProfile).map(_.map(_.result))
    }

  }

  case class ActionChoicePermission(
                                     actionChoice: ActionChoice,
                                     info: ActionsTriggeredInfo,
                                     maybeGroupVersion: Option[BehaviorGroupVersion],
                                     isActive: Boolean,
                                     canBeTriggered: Boolean,
                                     botProfile: SlackBotProfile,
                                     implicit val request: Request[AnyContent]
                                   ) extends ActionPermission {

    override val beQuiet: Boolean = actionChoice.shouldBeQuiet

    override val maybeResultText: Option[String] = if (isActive) {
      Some(s"$slackUser clicked ${actionChoice.label}")
    } else {
      Some("This skill has been updated, making these associated actions no longer valid")
    }

    override val shouldRemoveActions: Boolean = !actionChoice.allowMultipleSelections.exists(identity) && canBeTriggered

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]) = {
      if (canBeTriggered) {
        if (isActive) {
          for {
            maybeTs <- maybeInstantResponseTs
            result <- processTriggerableAndActiveActionChoice(actionChoice, maybeGroupVersion, info, botProfile, maybeTs)
          } yield result
        }
      } else {
        sendCannotBeTriggeredFor(actionChoice, maybeGroupVersion, info)
      }
    }
  }

  object ActionChoicePermission extends ActionPermissionType[ActionChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[ActionChoicePermission]] = {
      info.maybeSelectedActionChoice.map { actionChoice =>
        buildFor(actionChoice, info, botProfile)
      }
    }

    def buildFor(actionChoice: ActionChoice, info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[ActionChoicePermission] = {
      for {
        maybeOriginatingBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId)
        maybeGroupVersion <- Future.successful(maybeOriginatingBehaviorVersion.map(_.groupVersion))
        maybeActiveGroupVersion <- maybeGroupVersion.map { groupVersion =>
          dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(groupVersion.group, Conversation.SLACK_CONTEXT, info.channel.id)
        }.getOrElse(Future.successful(None))
        isActive <- (for {
          groupVersion <- maybeGroupVersion
          activeVersion <- maybeActiveGroupVersion
        } yield {
          if (groupVersion == activeVersion) {
            Future.successful(true)
          } else {
            dataService.behaviorGroupVersions.haveActionsWithNameAndSameInterface(actionChoice.actionName, groupVersion, activeVersion)
          }
        }).getOrElse(Future.successful(false))
        canBeTriggered <- for {
          maybeUser <- dataService.users.ensureUserFor(LoginInfo(Conversation.SLACK_CONTEXT, info.user.id), botProfile.teamId).map(Some(_))
          canBeTriggered <- maybeUser.map { user =>
            actionChoice.canBeTriggeredBy(user, info.slackTeamIdForUser, botProfile.slackTeamId, dataService)
          }.getOrElse(Future.successful(false))
        } yield canBeTriggered
      } yield ActionChoicePermission(
        actionChoice,
        info,
        maybeActiveGroupVersion,
        isActive,
        canBeTriggered,
        botProfile,
        request
      )
    }
  }

  trait InputChoicePermission extends ActionPermission {
    val choice: String
    val isConversationDone: Boolean
    val isIncorrectUser: Boolean
    val maybeResultText = Some(s"$slackUser chose $choice")
    val shouldRemoveActions = true

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]) = {
      if (isConversationDone) {
        updateActionsMessageFor(info, Some(s"This conversation is no longer active"), shouldRemoveActions = true)
      } else if (isIncorrectUser) {
        info.maybeUserIdForDataTypeChoice.foreach { correctUserId =>
          val correctUser = s"<@${correctUserId}>"
          sendEphemeralMessage(s"Only $correctUser can answer this", info)
        }
      } else {
        inputChoiceResultFor(choice, info)
      }
    }
  }

  case class DataTypeChoicePermission(
                                    choice: String,
                                    info: ActionsTriggeredInfo,
                                    isConversationDone: Boolean,
                                    implicit val request: Request[AnyContent]
                                  ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingDataTypeChoice
  }

  object DataTypeChoicePermission extends ActionPermissionType[DataTypeChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[DataTypeChoicePermission]] = {
      info.maybeDataTypeChoice.map { choice =>
        buildFor(choice, info)
      }
    }

    def buildFor(choice: String, info: ActionsTriggeredInfo)(implicit request: Request[AnyContent]): Future[DataTypeChoicePermission] = {
      for {
        isConversationDone <- info.isForDataTypeChoiceForDoneConversation
      } yield DataTypeChoicePermission(
        choice,
        info,
        isConversationDone,
        request
      )
    }

  }

  case class YesNoChoicePermission(
                                    choice: String,
                                    info: ActionsTriggeredInfo,
                                    isConversationDone: Boolean,
                                    implicit val request: Request[AnyContent]
                                  ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingYesNo
  }

  object YesNoChoicePermission extends ActionPermissionType[YesNoChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[YesNoChoicePermission]] = {
      info.maybeYesNoAnswer.map { value =>
        buildFor(value, info)
      }
    }

    def buildFor(value: String, info: ActionsTriggeredInfo)(implicit request: Request[AnyContent]): Future[YesNoChoicePermission] = {
      for {
        isConversationDone <- info.isForYesNoForDoneConversation
      } yield YesNoChoicePermission(
        value,
        info,
        isConversationDone,
        request
      )
    }

  }

  trait HelpPermission extends ActionPermission {
    val shouldRemoveActions: Boolean = false
    val isIncorrectTeam: Boolean
    val botProfile: SlackBotProfile

    def runForCorrectTeam: Unit

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit = {
      if (isIncorrectTeam) {
        dataService.teams.find(botProfile.teamId).flatMap { maybeTeam =>
          val teamText = maybeTeam.map { team => s" ${team.name}"}.getOrElse("")
          val msg = s"Only members of the${teamText} team can make this choice"
          sendEphemeralMessage(msg, info)
        }
      } else {
        runForCorrectTeam
      }
    }

  }

  trait HelpPermissionType[T <: HelpPermission, V] extends ActionPermissionType[T] {

    def maybeValueFor(info: ActionsTriggeredInfo): Option[V]
    def buildFor(
                  value: V,
                  info: ActionsTriggeredInfo,
                  isIncorrectTeam: Boolean,
                  botProfile: SlackBotProfile
                )(implicit request:  Request[AnyContent]): T

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[T]] = {
      maybeValueFor(info).map { v =>
        buildFor(v, info, botProfile)
      }
    }

    def buildFor(value: V, info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[T] = {
      for {
        maybeUser <- dataService.users.ensureUserFor(LoginInfo(Conversation.SLACK_CONTEXT, info.user.id), botProfile.teamId).map(Some(_))
        isAdmin <- maybeUser.map { user =>
          dataService.users.isAdmin(user)
        }.getOrElse(Future.successful(false))
        maybeAttemptingSlackTeamIds <- maybeUser.map { user =>
          dataService.users.maybeSlackTeamIdsFor(user)
        }.getOrElse(Future.successful(None))
      } yield {
        val isSameTeam = maybeAttemptingSlackTeamIds.exists(_.contains(botProfile.slackTeamId))
        val isIncorrectTeam = !info.team.isEnterpriseGrid && !isSameTeam && !isAdmin
        buildFor(value, info, isIncorrectTeam, botProfile)
      }
    }
  }

  case class HelpIndexPermission(
                                  index: Int,
                                  info: ActionsTriggeredInfo,
                                  isIncorrectTeam: Boolean,
                                  botProfile: SlackBotProfile,
                                  implicit val request: Request[AnyContent]
                                ) extends HelpPermission {

    val maybeResultText = Some(s"$slackUser clicked More help.")

    def runForCorrectTeam: Unit = {
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
        botProfile,
        info.channel.id,
        info.user.id,
        info.message_ts,
        info.maybeOriginalMessageThreadId,
        info.isEphemeral,
        Some(info.response_url),
        beQuiet = false
      )
    }

  }

  object HelpIndexPermission extends HelpPermissionType[HelpIndexPermission, Int] {

    def maybeValueFor(info: ActionsTriggeredInfo): Option[Int] = info.maybeHelpIndexAt
    def buildFor(
                  value: Int,
                  info: ActionsTriggeredInfo,
                  isIncorrectTeam: Boolean,
                  botProfile: SlackBotProfile
                )(implicit request:  Request[AnyContent]): HelpIndexPermission = {
      HelpIndexPermission(value, info, isIncorrectTeam, botProfile, request)
    }

  }

  case class HelpForSkillPermission(
                                     searchValue: HelpGroupSearchValue,
                                     info: ActionsTriggeredInfo,
                                     isIncorrectTeam: Boolean,
                                     botProfile: SlackBotProfile,
                                     implicit val request: Request[AnyContent]
                                   ) extends HelpPermission {

    val maybeResultText = Some(info.findButtonLabelForNameAndValue(SHOW_BEHAVIOR_GROUP_HELP, searchValue.helpGroupId).map { text =>
      s"$slackUser clicked $text."
    } getOrElse {
      s"$slackUser clicked a button."
    })

    def runForCorrectTeam: Unit = {
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
        botProfile,
        info.channel.id,
        info.user.id,
        info.message_ts,
        info.maybeOriginalMessageThreadId,
        info.isEphemeral,
        Some(info.response_url),
        beQuiet = false
      )
    }

  }

  object HelpForSkillPermission extends HelpPermissionType[HelpForSkillPermission, HelpGroupSearchValue] {

    def maybeValueFor(info: ActionsTriggeredInfo): Option[HelpGroupSearchValue] = info.maybeHelpForSkillIdWithMaybeSearch
    def buildFor(
                  value: HelpGroupSearchValue,
                  info: ActionsTriggeredInfo,
                  isIncorrectTeam: Boolean,
                  botProfile: SlackBotProfile
                )(implicit request:  Request[AnyContent]): HelpForSkillPermission = {
      HelpForSkillPermission(value, info, isIncorrectTeam, botProfile, request)
    }

  }

  case class HelpListAllActionsPermission(
                                          searchValue: HelpGroupSearchValue,
                                          info: ActionsTriggeredInfo,
                                          isIncorrectTeam: Boolean,
                                          botProfile: SlackBotProfile,
                                          implicit val request: Request[AnyContent]
                                          ) extends HelpPermission {

    val maybeResultText = Some(s"$slackUser clicked List all actions")

    def runForCorrectTeam: Unit = {
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
        botProfile,
        info.channel.id,
        info.user.id,
        info.message_ts,
        info.maybeOriginalMessageThreadId,
        info.isEphemeral,
        Some(info.response_url),
        beQuiet = false
      )
    }

  }

  object HelpListAllActionsPermission extends HelpPermissionType[HelpListAllActionsPermission, HelpGroupSearchValue] {

    def maybeValueFor(info: ActionsTriggeredInfo): Option[HelpGroupSearchValue] = info.maybeActionListForSkillId
    def buildFor(
                  value: HelpGroupSearchValue,
                  info: ActionsTriggeredInfo,
                  isIncorrectTeam: Boolean,
                  botProfile: SlackBotProfile
                )(implicit request: Request[AnyContent]): HelpListAllActionsPermission = {
      HelpListAllActionsPermission(value, info, isIncorrectTeam, botProfile, request)
    }

  }

  trait ConversationActionPermission extends ActionPermission {
    val correctUserId: String
    lazy val isCorrectUser: Boolean = correctUserId == info.user.id
    lazy val correctUser: String = s"<@$correctUserId>"
    lazy val shouldRemoveActions: Boolean = isCorrectUser

    def runForCorrectUser(): Unit

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit = {
      if (isCorrectUser) {
        runForCorrectUser()
      } else {
        sendEphemeralMessage(s"Only $correctUser can do this", info)
      }
    }
  }

  case class ConfirmContinueConversationPermission(
                                                    response: ConfirmContinueConversationResponse,
                                                    info: ActionsTriggeredInfo,
                                                    implicit val request: Request[AnyContent]
                                                  ) extends ConversationActionPermission {

    val maybeResultText: Option[String] = {
      val r = if (response.shouldContinue) { "Yes" } else { "No" }
      Some(s"$slackUser clicked '$r'")
    }

    val correctUserId: String = response.userId

    def continue(conversation: Conversation): Future[Unit] = {
      dataService.conversations.touch(conversation).flatMap { _ =>
        cacheService.getEvent(conversation.pendingEventKey).map { event =>
          slackEventService.onEvent(event)
        }.getOrElse(Future.successful({}))
      }
    }

    def dontContinue(conversation: Conversation): Future[Unit] = {
      dataService.conversations.background(conversation, "OK, on to the next thing.", includeUsername = false).flatMap { _ =>
        cacheService.getEvent(conversation.pendingEventKey).map { event =>
          eventHandler.handle(event, None).flatMap { results =>
            Future.sequence(
              results.map(result => services.botResultService.sendIn(result, None).map { _ =>
                Logger.info(event.logTextFor(result, None))
              })
            )
          }.map(_ => {})
        }.getOrElse(Future.successful({}))
      }
    }

    def runForCorrectUser(): Unit = {
      dataService.conversations.find(response.conversationId).flatMap { maybeConversation =>
        maybeConversation.map { convo =>
          if (response.shouldContinue) {
            continue(convo)
          } else {
            dontContinue(convo)
          }
        }.getOrElse(Future.successful({}))
      }
    }
  }

  object ConfirmContinueConversationPermission extends ActionPermissionType[ConfirmContinueConversationPermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[ConfirmContinueConversationPermission]] = {
      info.maybeConfirmContinueConversationResponse.map { response =>
        buildFor(response, info)
      }
    }

    def buildFor(response: ConfirmContinueConversationResponse, info: ActionsTriggeredInfo)(implicit request: Request[AnyContent]): Future[ConfirmContinueConversationPermission] = {
      Future.successful(ConfirmContinueConversationPermission(
        response,
        info,
        request
      ))
    }

  }

  case class StopConversationPermission(
                                         response: StopConversationResponse,
                                         info: ActionsTriggeredInfo,
                                         implicit val request: Request[AnyContent]
                                       ) extends ConversationActionPermission {

    val correctUserId: String = response.userId

    val maybeResultText = Some(s"$slackUser clicked 'Stop asking'")

    def runForCorrectUser(): Unit = {
      dataService.conversations.find(response.conversationId).flatMap { maybeConversation =>
        maybeConversation.map { convo =>
          dataService.conversations.cancel(convo)
        }.getOrElse(Future.successful({}))
      }
    }

  }

  object StopConversationPermission extends ActionPermissionType[StopConversationPermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[StopConversationPermission]] = {
      info.maybeStopConversationResponse.map { response =>
        buildFor(response, info)
      }
    }

    def buildFor(response: StopConversationResponse, info: ActionsTriggeredInfo)(implicit request: Request[AnyContent]): Future[StopConversationPermission] = {
      Future.successful(StopConversationPermission(
        response,
        info,
        request
      ))
    }

  }

  case class HelpRunBehaviorVersionPermission(
                                               behaviorVersionId: String,
                                               info: ActionsTriggeredInfo,
                                               isActive: Boolean,
                                               canBeTriggered: Boolean,
                                               botProfile: SlackBotProfile,
                                               implicit val request: Request[AnyContent]
                                             ) extends ActionPermission {

    val shouldRemoveActions: Boolean = false
    val maybeOptionLabel: Option[String] = info.findOptionLabelForValue(behaviorVersionId).map(_.mkString("“", "", "”"))
    val maybeResultText = Some({
      val actionText = maybeOptionLabel.getOrElse("an action")
      if (!isActive) {
        s"$slackUser tried to run an obsolete version of $actionText – run help again to get the latest actions"
      } else if (!canBeTriggered) {
        s"$slackUser tried to run $actionText"
      } else {
        info.findButtonLabelForNameAndValue(BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION, behaviorVersionId).map { text =>
          s"$slackUser clicked $text"
        }.getOrElse {
          s"$slackUser ran $actionText"
        }
      }
    })

    private def runBehaviorVersion(): Unit = {
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
              None,
              None,
              userExpectsResponse = true
            ).map(Some(_))
          }.getOrElse(Future.successful(None))
          maybeResult <- maybeResponse.map { response =>
            response.result.map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield maybeResult,
        botProfile,
        info.channel.id,
        info.user.id,
        info.message_ts,
        info.maybeOriginalMessageThreadId,
        info.isEphemeral,
        Some(info.response_url),
        beQuiet = false
      )
    }

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit = {
      if (canBeTriggered) {
        if (isActive) {
          runBehaviorVersion()
        }
      } else {
        dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId).flatMap { maybeBehaviorVersion =>
          val teamText = maybeBehaviorVersion.map { bv => s" ${bv.team.name}" }.getOrElse("")
          sendEphemeralMessage(s"Only members of the$teamText team can run this", info)
        }
      }

    }
  }

  object HelpRunBehaviorVersionPermission extends ActionPermissionType[HelpRunBehaviorVersionPermission] {

    def maybeFor(info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Option[Future[HelpRunBehaviorVersionPermission]] = {
      info.maybeHelpRunBehaviorVersionId.map { behaviorVersionId =>
        buildFor(behaviorVersionId, info, botProfile)
      }
    }

    def buildFor(behaviorVersionId: String, info: ActionsTriggeredInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[HelpRunBehaviorVersionPermission] = {
      for {
        maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId)
        isActive <- maybeBehaviorVersion.map { behaviorVersion =>
          dataService.behaviorGroupVersions.isActive(behaviorVersion.groupVersion, Conversation.SLACK_CONTEXT, info.channel.id)
        }.getOrElse(Future.successful(false))
        maybeUser <- dataService.users.ensureUserFor(LoginInfo(Conversation.SLACK_CONTEXT, info.user.id), botProfile.teamId).map(Some(_))
        canBeTriggered <- (for {
          behaviorVersion <- maybeBehaviorVersion
          user <- maybeUser
        } yield behaviorVersion.groupVersion.canBeTriggeredBy(user, dataService)).getOrElse(Future.successful(false))
      } yield {
        HelpRunBehaviorVersionPermission(
          behaviorVersionId,
          info,
          isActive = isActive,
          canBeTriggered = canBeTriggered,
          botProfile,
          request
        )
      }
    }

  }

  def action = Action.async { implicit request =>
    actionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      payload => {

        // Slack improperly (?) displays escaped text inside button labels as-is in the client when
        // we return the original message back.
        //
        // TODO: Investigate whether this is safe and/or desirable
        val unescapedPayload = SlackMessage.unescapeSlackHTMLEntities(payload)
        Json.parse(unescapedPayload).validate[ActionsTriggeredInfo] match {
          case JsSuccess(info, _) => {
            if (info.isValid) {
              info.maybeBotProfile.flatMap { maybeBotProfile =>
                maybeBotProfile.map { botProfile =>
                  DataTypeChoicePermission.maybeResultFor(info, botProfile).getOrElse {
                    YesNoChoicePermission.maybeResultFor(info, botProfile).getOrElse {
                      ActionChoicePermission.maybeResultFor(info, botProfile).getOrElse {
                        HelpIndexPermission.maybeResultFor(info, botProfile).getOrElse {
                          HelpForSkillPermission.maybeResultFor(info, botProfile).getOrElse {
                            HelpListAllActionsPermission.maybeResultFor(info, botProfile).getOrElse {
                              ConfirmContinueConversationPermission.maybeResultFor(info, botProfile).getOrElse {
                                StopConversationPermission.maybeResultFor(info, botProfile).getOrElse {
                                  HelpRunBehaviorVersionPermission.maybeResultFor(info, botProfile).getOrElse {
                                    Future.successful(Ok(""))
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }.getOrElse(Future.successful(Ok("")))
              }
            } else {
              Future.successful(Forbidden("Bad token"))
            }
          }
          case JsError(err) => {
            Future.successful(BadRequest(err.toString))
          }
        }
      }
    )
  }


}
