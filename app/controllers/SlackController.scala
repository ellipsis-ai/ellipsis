package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import javax.inject.Inject
import json.Formatting._
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{Normal, Threaded}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events._
import models.behaviors.events.slack._
import models.behaviors.{ActionChoice, BotResult, SimpleTextResult}
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Environment, Logger, Mode}
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
                                ) extends EllipsisController with ChatPlatformController {

  override type ActionsTriggeredInfoType = SlackActionsTriggeredInfo
  override type BotProfileType = SlackBotProfile

  val configuration = services.configuration
  val lambdaService = services.lambdaService
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
        Ok(views.html.auth.addToSlack(viewConfig(None), scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index()))
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

  def sendEphemeralMessageFor(message: String, slackTeamId: String, slackChannelId: String, maybeThreadTs: Option[String], slackUserId: String): Future[Unit] = {
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

  def sendEphemeralMessage(message: String, info: MessageRequestInfo): Future[Unit] = {
    info.slackTeamIdsForBots.headOption.map { slackTeamId =>
      sendEphemeralMessageFor(message, slackTeamId, info.channel, info.maybeThreadTs, info.userId)
    }.getOrElse(Future.successful({}))
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
    slackEventService.isUserValidForBot(info.userId, botProfile, info.maybeEnterpriseId).flatMap { isUserValidForBot =>
      if (isUserValidForBot) {
        for {
          maybeSlackMessage <- SlackMessage.maybeFromMessageTs(info.event.item.ts, info.event.item.channel, botProfile, services)
          result <- {
            val event = SlackReactionAddedEvent(
              SlackEventContext(
                botProfile,
                info.channel,
                maybeThreadId = maybeSlackMessage.flatMap(_.maybeThreadId),
                info.userId
              ),
              info.event.reaction,
              maybeSlackMessage
            )
            slackEventService.onEvent(event)
          }
        } yield result
      } else {
        Future.successful({})
      }
    }
  }

  private def processMessageEventsFor(info: MessageRequestInfo, botProfile: SlackBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
      slackMessage <- SlackMessage.fromFormattedText(info.message, botProfile, slackEventService, Some(info.ts), info.maybeThreadTs)
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
            Some(info.ts),
            maybeOriginalEventType = None,
            maybeScheduled = None,
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
            processReactionEventsFor(info, profile)
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
      slackMessage <- SlackMessage.fromFormattedText(info.text, botProfile, slackEventService, None, None)
      event <- Future.successful(SlashCommandEvent(
        SlackEventContext(
          botProfile,
          info.channelId,
          maybeThreadId = None, // You can't use slash commands in threads
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
    def maybeValue: Future[Option[String]] = {
      value.map { v =>
        cacheService.getSlackActionValue(v).map { maybeV =>
          maybeV.orElse(value)
        }
      }.getOrElse(Future.successful(value))
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
  case class SlackActionsTriggeredInfo(
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
                                 ) extends RequestInfo with ActionsTriggeredInfo {

    val channelId: String = channel.id
    val teamIdForContext: String = slackTeamIdForBot
    val teamIdForUserForContext: String = slackTeamIdForUser

    val contextName: String = Conversation.SLACK_CONTEXT

    def loginInfo: LoginInfo = LoginInfo(contextName, user.id)
    def otherLoginInfos: Seq[LoginInfo] = Seq()

    def onEvent(event: Event): Future[Unit] = slackEventService.onEvent(event)

    def sendResultWithNewEvent(
                                description: String,
                                maybeOriginalEventType: Option[EventType],
                                getEventualMaybeResult: MessageEvent => Future[Option[BotResult]],
                                botProfile: BotProfileType,
                                beQuiet: Boolean
                              ): Future[Option[String]] = {
      dataService.slackBotProfiles.sendResultWithNewEvent(
        "help index",
        getEventualMaybeResult,
        botProfile,
        channel.id,
        user.id,
        message_ts,
        maybeOriginalEventType,
        maybeOriginalMessageThreadId,
        isEphemeral,
        Some(response_url),
        beQuiet = false
      )

    }

    def inputChoiceResultFor(value: String, maybeResultText: Option[String])(implicit request: Request[AnyContent]): Future[Unit] = {
      for {
        maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(team.id).map(_.headOption)
        maybeSlackMessage <- maybeProfile.map { profile =>
          SlackMessage.fromFormattedText(value, profile, slackEventService, None, None).map(Some(_))
        }.getOrElse(Future.successful(None))
        _ <- (for {
          profile <- maybeProfile
          slackMessage <- maybeSlackMessage
        } yield {
          slackEventService.onEvent(SlackMessageEvent(
            SlackEventContext(
              profile,
              channel.id,
              maybeOriginalMessageThreadId,
              user.id
            ),
            slackMessage,
            None,
            Some(message_ts),
            maybeOriginalEventType = None,
            maybeScheduled = None,
            isUninterruptedConversation = false,
            isEphemeral,
            Some(response_url),
            beQuiet = false
          ))
        }).getOrElse {
          Future.successful({})
        }
      } yield {}
    }

    def sendEphemeralMessage(message: String): Future[Unit] = {
      sendEphemeralMessageFor(message, slackTeamIdForBot, channel.id, maybeOriginalMessageThreadId, user.id)
    }

    def updateActionsMessageFor(
                                 maybeResultText: Option[String],
                                 shouldRemoveActions: Boolean,
                                 botProfile: BotProfileType
                               ): Future[Unit] = {
      original_message.map { originalMessage =>
        val maybeOriginalColor = originalMessage.attachments.headOption.flatMap(_.color)
        val newAttachment = AttachmentInfo(maybeResultText, None, None, Some(Seq("text")), Some(callback_id), color = maybeOriginalColor, footer = maybeResultText)
        val originalAttachmentsToUse = if (shouldRemoveActions) {
          originalMessage.attachments.map(ea => ea.copy(actions = None))
        } else {
          originalMessage.attachments
        }
        val updated = originalMessage.copy(attachments = originalAttachmentsToUse :+ newAttachment)
        for {
          maybeProfile <- dataService.slackBotProfiles.allForSlackTeamId(team.id).map(_.headOption)
          _ <- (for {
            profile <- maybeProfile
          } yield {
            ws.
              url("https://slack.com/api/chat.update").
              withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
              post(Map(
                "token" -> Seq(profile.token),
                "channel" -> Seq(channel.id),
                "text" -> Seq(updated.text),
                "attachments" -> Seq(Json.prettyPrint(Json.toJson(updated.attachments))),
                "as_user" -> Seq("true"),
                "ts" -> Seq(message_ts),
                "user" -> Seq(user.id)
              ))
          }).getOrElse {
            Future.successful({})
          }
        } yield {}
      }.getOrElse {
        Future.successful({})
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

    def sendCannotBeTriggeredFor(
                                  actionChoice: ActionChoice,
                                  maybeGroupVersion: Option[BehaviorGroupVersion]
                                ): Future[Unit] = {
      for {
        maybeChoiceSlackUserId <- maybeSlackUserIdForActionChoice(actionChoice)
        _ <- {
          val msg = cannotBeTriggeredMessageFor(actionChoice, maybeGroupVersion, maybeChoiceSlackUserId)
          sendEphemeralMessage(msg)
        }
      } yield {}
    }

    def processTriggerableAndActiveActionChoice(
                                                 actionChoice: ActionChoice,
                                                 maybeGroupVersion: Option[BehaviorGroupVersion],
                                                 botProfile: SlackBotProfile,
                                                 maybeInstantResponseTs: Option[String]
                                               ): Future[Unit] = {
      for {
        maybeThreadIdToUse <- maybeOriginalMessageThreadId.map(tid => Future.successful(Some(tid))).getOrElse {
          dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId).map { maybeOriginatingBehaviorVersion =>
            if (maybeOriginatingBehaviorVersion.exists(_.responseType == Threaded)) {
              maybeInstantResponseTs.orElse(original_message.map(_.ts))
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
                userExpectsResponse = true,
                maybeMessageListener = None
              ).map(Some(_))
            }.getOrElse(Future.successful(None))
            maybeResult <- maybeResponse.map { response =>
              response.result.map(Some(_))
            }.getOrElse(Future.successful(None))
          } yield maybeResult,
          botProfile,
          channel.id,
          user.id,
          message_ts,
          Some(EventType.actionChoice),
          maybeThreadIdToUse,
          isEphemeral,
          Some(response_url),
          actionChoice.shouldBeQuiet
        )
      } yield {}
    }

    def instantBackgroundResponse(responseText: String, permission: ActionPermission): Future[Option[String]] = {
      val trimmed = responseText.trim.replaceAll("(^\\u00A0|\\u00A0$)", "")
      val useEphemeralResponse = isEphemeral || permission.beQuiet
      if (trimmed.isEmpty) {
        Future.successful(None)
      } else {
        for {
          maybeBotProfile <- maybeBotProfile
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
              channel.id,
              user.id,
              message_ts,
              None,
              maybeOriginalMessageThreadId,
              useEphemeralResponse,
              Some(response_url),
              beQuiet = false
            )
          }.getOrElse(Future.successful(None))
        } yield maybeTs
      }
    }

    def result(maybeResultText: Option[String], permission: ActionPermission): Result = {

      // respond immediately by sending a new message
      val instantResponse = maybeResultText.map(t => instantBackgroundResponse(t, permission)).getOrElse(Future.successful(None))
      permission.runInBackground(instantResponse)

      val updated = if (permission.shouldRemoveActions) {
        original_message.map { originalMessage =>
          val maybeOriginalColor = originalMessage.attachments.headOption.flatMap(_.color)
          val newAttachment = AttachmentInfo(
            maybeResultText,
            title = None,
            text = None,
            Some(Seq("text")),
            Some(callback_id),
            color = maybeOriginalColor,
            footer = Some("✔︎︎ OK")
          )
          val attachments = originalMessage.attachments.map(ea => ea.copy(actions = None))
          originalMessage.copy(attachments = attachments :+ newAttachment)
        }
      } else {
        original_message
      }
      updated.map { u =>
        Ok(Json.toJson(u))
      }.getOrElse(Ok(""))
    }

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

    def maybeHelpForSkillIdWithMaybeSearch: Future[Option[HelpGroupSearchValue]] = {
      val maybeAction = actions.find(_.name == SHOW_BEHAVIOR_GROUP_HELP)
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
          maybeValue.map(HelpGroupSearchValue.fromString)
        }
      }.getOrElse(Future.successful(None))
    }

    def maybeActionListForSkillId: Future[Option[HelpGroupSearchValue]] = {
      val maybeAction = actions.find(_.name == LIST_BEHAVIOR_GROUP_ACTIONS)
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
          maybeValue.map(HelpGroupSearchValue.fromString)
        }
      }.getOrElse(Future.successful(None))
    }

    def maybeDataTypeChoice: Future[Option[String]] = {
      val maybeSlackUserId = maybeUserIdForCallbackId(DATA_TYPE_CHOICE, callback_id)
      val maybeAction = maybeSlackUserId.flatMap { slackUserId =>
        if (user.id == slackUserId) {
          actions.headOption
        } else {
          None
        }
      }
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
          maybeValue.orElse {
            for {
              selectedOptions <- action.selected_options
              firstOption <- selectedOptions.headOption
            } yield firstOption.value
          }
        }
      }.getOrElse(Future.successful(None))
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

    def maybeHelpIndexAt: Future[Option[Int]] = {
      val maybeAction = actions.find { info => info.name == SHOW_HELP_INDEX }
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
          maybeValue.map { value =>
            try {
              value.toInt
            } catch {
              case _: NumberFormatException => 0
            }
          }.orElse(Some(0))
        }
      }.getOrElse(Future.successful(None))
    }

    val maybeConfirmContinueConversationId: Option[String] = maybeConversationIdForCallbackId(CONFIRM_CONTINUE_CONVERSATION, callback_id)

    val maybeConfirmContinueConversationUserId: Option[String] = maybeUserIdForCallbackId(CONFIRM_CONTINUE_CONVERSATION, callback_id)

    def maybeConfirmContinueConversationAnswer: Future[Option[Boolean]] = {
      val maybeAction = actions.find(_.name == callback_id)
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
          maybeValue.filter(v => v == YES || v == NO).map(_ == YES)
        }
      }.getOrElse(Future.successful(None))
    }

    def maybeConfirmContinueConversationResponse: Future[Option[ConfirmContinueConversationResponse]] = {
      maybeConfirmContinueConversationAnswer.map { maybeConfirmContinueConversationAnswerValue =>
        for {
          conversationId <- maybeConfirmContinueConversationId
          userId <- maybeConfirmContinueConversationUserId
          value <- maybeConfirmContinueConversationAnswerValue
        } yield ConfirmContinueConversationResponse(value, conversationId, userId)
      }
    }

    val maybeStopConversationId: Option[String] = maybeConversationIdForCallbackId(STOP_CONVERSATION, callback_id)

    val maybeStopConversationUserId: Option[String] = maybeUserIdForCallbackId(STOP_CONVERSATION, callback_id)

    val maybeStopConversationResponse: Option[StopConversationResponse] = {
      for {
        conversationId <- maybeStopConversationId
        userId <- maybeStopConversationUserId
      } yield StopConversationResponse(conversationId, userId)
    }

    def maybeHelpRunBehaviorVersionId: Future[Option[String]] = {
      val maybeAction = actions.find(_.name == BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION)
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
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
      }.getOrElse(Future.successful(None))
    }

    def maybeSelectedActionChoice: Future[Option[ActionChoice]] = {
      val maybeAction = actions.find(_.name == ACTION_CHOICE)
      maybeAction.map { action =>
        action.maybeValue.map { maybeValue =>
          maybeValue.flatMap { value =>
            Try(Json.parse(value)) match {
              case Success(json) => json.asOpt[ActionChoice]
              case Failure(_) => None
            }
          }
        }
      }.getOrElse(Future.successful(None))
    }

    def maybeYesNoAnswer: Future[Option[String]] = {
      val maybeSlackUserId = maybeUserIdForCallbackId(YES_NO_CHOICE, callback_id)
      val maybeAction = maybeSlackUserId.flatMap { slackUserId =>
        if (user.id == slackUserId) {
          actions.find(_.name == callback_id)
        } else {
          None
        }
      }
      maybeAction.map { action =>
        action.maybeValue
      }.getOrElse(Future.successful(None))
    }

    def maybeTextInputAnswer: Option[String] = None

    def isForTextInputForDoneConversation: Future[Boolean] = Future.successful(false)

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

    def isIncorrectTeam(botProfile: BotProfileType): Future[Boolean] = {
      for {
        maybeUser <- dataService.users.ensureUserFor(loginInfo, otherLoginInfos, botProfile.teamId).map(Some(_))
        isAdmin <- maybeUser.map { user =>
          dataService.users.isAdmin(user)
        }.getOrElse(Future.successful(false))
        maybeAttemptingSlackTeamIds <- maybeUser.map { user =>
          dataService.users.maybeSlackTeamIdsFor(user)
        }.getOrElse(Future.successful(None))
      } yield {
        val isSameTeam = maybeAttemptingSlackTeamIds.exists(_.contains(botProfile.teamIdForContext))
        !team.isEnterpriseGrid && !isSameTeam && !isAdmin
      }
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

    def formattedUserFor(permission: ActionPermission): String = {
      if (channel.isDirectMessage || permission.beQuiet) {
        "You"
      } else {
        s"<@${user.id}>"
      }
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

  implicit val actionsTriggeredReads = Json.reads[SlackActionsTriggeredInfo]

  private def maybeSlackUserIdForActionChoice(actionChoice: ActionChoice): Future[Option[String]] = {
    dataService.users.find(actionChoice.userId).flatMap { maybeUser =>
      maybeUser.map { user =>
        dataService.linkedAccounts.maybeSlackUserIdFor(user)
      }.getOrElse(Future.successful(None))
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
        Json.parse(unescapedPayload).validate[SlackActionsTriggeredInfo] match {
          case JsSuccess(info, _) => {
            if (info.isValid) {
              for {
                maybeBotProfile <- info.maybeBotProfile
                maybeResult <- maybeBotProfile.map { botProfile =>
                  maybePermissionResultFor(info, botProfile)
                }.getOrElse(Future.successful(None))
              } yield {
                maybeResult.getOrElse(Ok(""))
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
