package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import json.Formatting._
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.Normal
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events._
import models.behaviors.events.ms_teams.MSTeamsMessageEvent
import models.behaviors.{ActionChoice, BotResult, SimpleTextResult}
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Environment, Logger, Mode}
import services._
import services.ms_teams.apiModels._
import services.ms_teams.apiModels.Formatting._
import services.ms_teams.{MSTeamsApiService, MSTeamsEventService}

import scala.concurrent.{ExecutionContext, Future}

class MSTeamsController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val eventHandler: EventHandler,
                                  val services: DefaultServices,
                                  val assetsProvider: Provider[RemoteAssets],
                                  val environment: Environment,
                                  val eventService: MSTeamsEventService,
                                  val apiService: MSTeamsApiService,
                                  implicit val ec: ExecutionContext
                                ) extends EllipsisController with ChatPlatformController {

  val configuration = services.configuration
  val lambdaService = services.lambdaService
  val ws = services.ws
  val botResultService = services.botResultService
  implicit val actorSystem = services.actorSystem

  override type BotProfileType = MSTeamsBotProfile
  override type ActionsTriggeredInfoType = MSTeamsActionsTriggeredInfo

  case class MSTeamsActionsTriggeredInfo(
                                          `type`: String,
                                          id: String,
                                          timestamp: String,
                                          serviceUrl: String,
                                          channelId: String,
                                          from: MessageParticipantInfo,
                                          conversation: ConversationInfo,
                                          recipient: MessageParticipantInfo,
                                          textFormat: Option[String],
                                          text: Option[String],
                                          value: Option[JsObject],
                                          replyToId: Option[String],
                                          channelData: ChannelDataInfo
                                         ) extends ActionsTriggeredInfo {

    val maybeTenantId: Option[String] = channelData.tenant.map(_.id)

    def toActivityInfo: ActivityInfo = ActivityInfo(
      id,
      serviceUrl,
      from,
      conversation,
      recipient,
      text,
      channelData
    )

    def maybeKeyMatching(keyPrefix: String): Option[String] = {
      value.flatMap { v =>
        v.value.keys.find(_.startsWith(keyPrefix))
      }
    }
    def maybeValueResultMatching(keyPrefix: String): Option[JsLookupResult] = {
      for {
        v <- value
        k <- maybeKeyMatching(keyPrefix)
      } yield (v \ k)
    }
    val contextName: String = Conversation.MS_TEAMS_CONTEXT
    def findButtonLabelForNameAndValue(name: String,value: String): Option[String] = None
    def findOptionLabelForValue(value: String): Option[String] = None
    def formattedUserFor(permission: ActionPermission): String = {
      if (conversation.conversationType == "personal" || permission.beQuiet) {
        "You"
      } else {
        s"@${from.name}"
      }

    }
    def inputChoiceResultFor(value: String, maybeResultText: Option[String])(implicit request: Request[AnyContent]): Future[Unit] = {
      for {
        maybeProfile <- dataService.msTeamsBotProfiles.find(teamIdForContext).map(_.headOption)
        _ <- (for {
          profile <- maybeProfile
        } yield {
          deleteMessage(profile)
          eventService.onEvent(MSTeamsMessageEvent(
            MSTeamsEventContext(
              profile,
              this.toActivityInfo
            ),
            value,
            None,
            isUninterruptedConversation = false,
            isEphemeral = false,
            None,
            beQuiet = false
          ))
        }).getOrElse {
          Future.successful({})
        }
      } yield {}
    }
    def maybeBotProfile: Future[Option[MSTeamsBotProfile]] = {
      maybeTenantId.map { tid =>
        dataService.msTeamsBotProfiles.find(tid)
      }.getOrElse(Future.successful(None))
    }
    def instantBackgroundResponse(responseText: String, permission: ActionPermission): Future[Option[String]] = {
      val trimmed = responseText.trim.replaceAll("(^\\u00A0|\\u00A0$)", "")
      if (trimmed.isEmpty) {
        Future.successful(None)
      } else {
        for {
          maybeBotProfile <- maybeBotProfile
          maybeTs <- maybeBotProfile.map { botProfile =>
            sendResultWithNewEvent(
              "Message acknowledging response to MS Teams action",
              messageEvent => for {
                maybeConversation <- messageEvent.maybeOngoingConversation(dataService)
              } yield {
                Some(SimpleTextResult(
                  messageEvent,
                  maybeConversation,
                  s"_${trimmed}_",
                  responseType = Normal,
                  shouldInterrupt = false
                ))
              },
              botProfile,
              beQuiet = false
            )
          }.getOrElse(Future.successful(None))
        } yield maybeTs
      }
    }
    def isForDataTypeChoiceForDoneConversation: Future[Boolean] = {
      maybeKeyMatching(DATA_TYPE_CHOICE).map { key =>
        maybeConversationIdForCallbackId(DATA_TYPE_CHOICE, key).map { convoId =>
          dataService.conversations.find(convoId).map { maybeConvo =>
            maybeConvo.exists(_.isDone)
          }
        }.getOrElse(Future.successful(false))
      }.getOrElse(Future.successful(false))
    }
    def isForYesNoForDoneConversation: Future[Boolean] = Future.successful(false)
    def isForTextInputForDoneConversation: Future[Boolean] = Future.successful(false)
    def isIncorrectTeam(botProfile: BotProfileType): Future[Boolean] = Future.successful(false)
    def isIncorrectUserTryingDataTypeChoice: Boolean = false
    def isIncorrectUserTryingYesNo: Boolean = false
    def maybeActionListForSkillId: Option[HelpGroupSearchValue] = {
      maybeValueResultMatching(LIST_BEHAVIOR_GROUP_ACTIONS).flatMap(_.asOpt[String]).map(HelpGroupSearchValue.fromString)
    }
    val maybeConfirmContinueConversationResponse: Option[ConfirmContinueConversationResponse] = None
    def maybeDataTypeChoice: Option[String] = maybeValueResultMatching(DATA_TYPE_CHOICE).flatMap(_.asOpt[String])
    def maybeHelpForSkillIdWithMaybeSearch: Option[HelpGroupSearchValue] = {
      maybeValueResultMatching(SHOW_BEHAVIOR_GROUP_HELP).flatMap(_.asOpt[String]).map(HelpGroupSearchValue.fromString)
    }
    def maybeHelpIndexAt: Option[Int] = maybeValueResultMatching(SHOW_BEHAVIOR_GROUP_HELP).flatMap(_.asOpt[Int])
    def maybeHelpRunBehaviorVersionId: Option[String] = maybeValueResultMatching(BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION).flatMap(_.asOpt[String])
    def maybeSelectedActionChoice: Option[ActionChoice] = maybeValueResultMatching(ACTION_CHOICE).flatMap(_.asOpt[ActionChoice])
    val maybeStopConversationResponse: Option[StopConversationResponse] = None
    val maybeUserIdForDataTypeChoice: Option[String] = None
    def maybeYesNoAnswer: Option[String] = maybeValueResultMatching(YES_NO_CHOICE).flatMap(_.asOpt[String])
    def maybeTextInputAnswer: Option[String] = maybeValueResultMatching(TEXT_INPUT).flatMap(_.asOpt[String])
    def onEvent(event: Event): Future[Unit] = Future.successful({})
    def processTriggerableAndActiveActionChoice(
                                                 actionChoice: ActionChoice,
                                                 maybeGroupVersion: Option[BehaviorGroupVersion],
                                                 botProfile: BotProfileType,
                                                 maybeInstantResponseTs: Option[String]
                                               ): Future[Unit] = {
      for {
        _ <- sendResultWithNewEvent(
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
          actionChoice.shouldBeQuiet
        )
      } yield {}
    }
    def result(maybeResultText: Option[String], permission: ActionPermission): Result = {
      val instantResponse = maybeResultText.map(t => instantBackgroundResponse(t, permission)).getOrElse(Future.successful(None))
      permission.runInBackground(instantResponse)
      Ok("")
    }
    def sendCannotBeTriggeredFor(actionChoice: ActionChoice, maybeGroupVersion: Option[BehaviorGroupVersion]): Future[Unit] = Future.successful({})
    def sendEphemeralMessage(message: String): Future[Unit] = Future.successful({})
    def sendResultWithNewEvent(
                                description: String,
                                getEventualMaybeResult: MessageEvent => Future[Option[BotResult]],
                                botProfile: BotProfileType,
                                beQuiet: Boolean
                              ): Future[Option[String]] = {
      dataService.msTeamsBotProfiles.sendResultWithNewEvent(
        description,
        getEventualMaybeResult,
        botProfile,
        toActivityInfo,
        conversation.id,
        from.id,
        id,
        None,
        isEphemeral = false,
        beQuiet
      )
    }
    val teamIdForContext: String = maybeTenantId.get // TODO: hm
    val teamIdForUserForContext: String = teamIdForContext

    def updateActionsMessageFor(maybeResultText: Option[String], shouldRemoveActions: Boolean, botProfile: MSTeamsBotProfile): Future[Unit] = {
      if (shouldRemoveActions) {
        replyToId.map { rtid =>
          val client = apiService.profileClientFor(botProfile)
          val updated = ResponseInfo(
            "message",
            from = recipient,
            conversation,
            recipient = from,
            maybeResultText.getOrElse("updated"),
            "markdown",
            rtid,
            Some(Seq())
          )
          client.updateMessage(serviceUrl, conversation.id, rtid, Json.toJson(updated)).map(_ => {})
        }.getOrElse(Future.successful({}))
      } else {
        Future.successful({})
      }
    }

    def deleteMessage(botProfile: MSTeamsBotProfile): Future[Unit] = {
      replyToId.map { rtid =>
        val client = apiService.profileClientFor(botProfile)
        client.deleteMessage(serviceUrl, conversation.id, rtid).map(_ => {})
      }.getOrElse(Future.successful({}))
    }

    val userIdForContext: String = from.id

  }

  private def processMessageEventsFor(info: MSTeamsActionsTriggeredInfo, botProfile: MSTeamsBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
        // TODO: in the Slack case we check here if the user is allowed to invoke the bot. I think most of the reasons
        // don't apply in MS Teams, but we should check at some point what happens with e.g. messages from other bots
        result <- eventService.onEvent(
          MSTeamsMessageEvent(
            MSTeamsEventContext(
              botProfile,
              info.toActivityInfo
            ),
            info.text.getOrElse(""), // TODO: formatting
            None,
            isUninterruptedConversation = false,
            isEphemeral = false,
            None,
            beQuiet = false
          )
        )
    } yield result
  }

  private def messageEventResult(info: MSTeamsActionsTriggeredInfo)(implicit request: Request[AnyContent]): Result = {
    for {
      maybeProfile <- info.maybeTenantId.map(id => dataService.msTeamsBotProfiles.find(id)).getOrElse(Future.successful(None))
      _ <- maybeProfile.map { profile =>
        maybePermissionResultFor(info, profile).getOrElse {
          processMessageEventsFor(info, profile)
        }
      }.getOrElse(Future.successful({}))
    } yield {}

    // respond immediately
    Ok(":+1:")
  }
  implicit lazy val actionsTriggeredInfoFormat = Json.format[MSTeamsActionsTriggeredInfo]

  private def maybeMessageResult(implicit request: Request[AnyContent]): Option[Result] = {
    request.body.asJson.flatMap { json =>
      json.validate[MSTeamsActionsTriggeredInfo] match {
        case JsError(errors) => {
          Logger.info(s"Can't process MS Teams request:\n${errors.toString}")
          None
        }
        case JsSuccess(info, _) => {
          Some(messageEventResult(info))
        }
      }
    }
  }

  def event = Action { implicit request =>
    if (environment.mode == Mode.Dev) {
      Logger.info(s"MS Teams event received:\n${Json.prettyPrint(request.body.asJson.get)}")
    }

    maybeMessageResult.getOrElse {
      Ok("I don't know what to do with this request but I'm not concerned")
    }
  }

  def add = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.auth.addToMSTeams(viewConfig(None)))
  }

}
