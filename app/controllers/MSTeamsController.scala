package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
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
import play.api.{Environment, Logger}
import services._
import services.ms_teams.apiModels.Formatting._
import services.ms_teams.apiModels._
import services.ms_teams.{MSTeamsApiService, MSTeamsEventService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

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
                                          conversation: ConversationAccount,
                                          recipient: MessageParticipantInfo,
                                          textFormat: Option[String],
                                          text: Option[String],
                                          attachments: Option[Seq[Attachment]],
                                          value: Option[JsObject],
                                          replyToId: Option[String],
                                          channelData: ChannelDataInfo,
                                          entities: Option[JsValue]
                                         ) extends ActionsTriggeredInfo {

    val emojiRegex = new Regex("""<img alt=\"(.+?)\" class=\"emojione\".+?>""", "altText")

    private def contentFromHtml(str: String): String = {
      val withoutDivs = """<div>|</div>""".r.replaceAllIn(str, "")
      val withEmojiAltText = emojiRegex.replaceAllIn(withoutDivs, m => m.group("altText"))
      withEmojiAltText
    }

    val maybeHtmlText: Option[String] = attachments.flatMap { att =>
      att.find(_.isHtml).flatMap(_.content match {
        case Some(UnknownAttachmentContent(str: JsString)) => Some(contentFromHtml(str.value.trim))
        case _ => None
      })
    }

    val maybeTextToUse: Option[String] = maybeHtmlText.orElse(text)

    val maybeTenantId: Option[String] = channelData.tenant.map(_.id)

    def toActivityInfo: ActivityInfo = ActivityInfo(
      id,
      serviceUrl,
      from,
      conversation,
      recipient,
      maybeTextToUse,
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
        actionName <- (v \ "actionName").asOpt[String]
        r <- if (actionName == k) {
          Some(v \ k)
        } else {
          None
        }
      } yield r
    }
    val contextName: String = Conversation.MS_TEAMS_CONTEXT
    def findButtonLabelForNameAndValue(name: String,value: String): Option[String] = None
    def findOptionLabelForValue(value: String): Option[String] = None
    def formattedUserFor(permission: ActionPermission): String = {
      if (conversation.conversationType == "personal" || permission.beQuiet) {
        "You"
      } else {
        s"<at>${from.name}</at>"
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
            Seq(),
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
    private def isForKeyForDoneConversations(keyName: String): Future[Boolean] = {
      maybeKeyMatching(keyName).map { key =>
        maybeConversationIdForCallbackId(keyName, key).map { convoId =>
          dataService.conversations.find(convoId).map { maybeConvo =>
            maybeConvo.exists(_.isDone)
          }
        }.getOrElse(Future.successful(false))
      }.getOrElse(Future.successful(false))
    }
    def isForDataTypeChoiceForDoneConversation: Future[Boolean] = isForKeyForDoneConversations(DATA_TYPE_CHOICE)
    def isForYesNoForDoneConversation: Future[Boolean] = isForKeyForDoneConversations(YES_NO_CHOICE)
    def isForTextInputForDoneConversation: Future[Boolean] = isForKeyForDoneConversations(TEXT_INPUT)
    def isIncorrectTeam(botProfile: BotProfileType): Future[Boolean] = Future.successful(false)
    def isIncorrectUserTryingDataTypeChoice: Boolean = false
    def isIncorrectUserTryingYesNo: Boolean = false
    def maybeActionListForSkillId: Future[Option[HelpGroupSearchValue]] = {
      Future.successful(maybeValueResultMatching(LIST_BEHAVIOR_GROUP_ACTIONS).flatMap(_.asOpt[String]).map(HelpGroupSearchValue.fromString))
    }
    val maybeConfirmContinueConversationResponse: Future[Option[ConfirmContinueConversationResponse]] = Future.successful(None)
    def maybeDataTypeChoice: Future[Option[String]] = Future.successful(maybeValueResultMatching(DATA_TYPE_CHOICE).flatMap(_.asOpt[String]))
    def maybeHelpForSkillIdWithMaybeSearch: Future[Option[HelpGroupSearchValue]] = {
      Future.successful(maybeValueResultMatching(SHOW_BEHAVIOR_GROUP_HELP).flatMap(_.asOpt[String]).map(HelpGroupSearchValue.fromString))
    }
    def maybeHelpIndexAt: Future[Option[Int]] = {
      Future.successful(
        maybeValueResultMatching(SHOW_HELP_INDEX).map { value =>
          value.asOpt[String].map { idx =>
            try {
              idx.toInt
            } catch {
              case _: NumberFormatException => 0
            }
          }.getOrElse(0)
        }
      )
    }
    def maybeHelpRunBehaviorVersionId: Future[Option[String]] = Future.successful(maybeValueResultMatching(BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION).flatMap(_.asOpt[String]))
    def maybeSelectedActionChoice: Future[Option[ActionChoice]] = Future.successful(maybeValueResultMatching(ACTION_CHOICE).flatMap(_.asOpt[ActionChoice]))
    val maybeStopConversationResponse: Option[StopConversationResponse] = None
    val maybeUserIdForDataTypeChoice: Option[String] = None
    def maybeYesNoAnswer: Future[Option[String]] = Future.successful(maybeValueResultMatching(YES_NO_CHOICE).flatMap(_.asOpt[String]))
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
          val updated = ResponseInfo.newForMessage(
            recipient,
            conversation,
            Some(from),
            maybeResultText.getOrElse("updated"),
            "markdown",
            Some(rtid),
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

    def loginInfo: LoginInfo = LoginInfo(Conversation.MS_TEAMS_CONTEXT, from.id)
    def otherLoginInfos: Seq[LoginInfo] = from.aadObjectId.map { id => LoginInfo(Conversation.MS_AAD_CONTEXT, id) }.toSeq

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
            info.maybeTextToUse.getOrElse(""), // TODO: formatting
            info.attachments.getOrElse(Seq()),
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
        maybePermissionResultFor(info, profile).flatMap { maybeResult =>
          maybeResult.map(r => Future.successful({})).getOrElse {
            processMessageEventsFor(info, profile)
          }
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
    Logger.info(s"MS Teams event received:\n${Json.prettyPrint(request.body.asJson.get)}")

    maybeMessageResult.getOrElse {
      Ok("I don't know what to do with this request but I'm not concerned")
    }
  }

  def add = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.auth.addToMSTeams(viewConfig(None)))
  }

}
