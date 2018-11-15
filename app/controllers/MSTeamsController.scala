package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.events.ms_teams.MSTeamsMessageEvent
import models.behaviors.{ActionArg, ActionChoice, BotResult}
import models.help.HelpGroupSearchValue
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Environment, Logger, Mode}
import services._
import services.ms_teams.MSTeamsEventService
import services.ms_teams.apiModels._

import scala.concurrent.{ExecutionContext, Future}

class MSTeamsController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val eventHandler: EventHandler,
                                  val services: DefaultServices,
                                  val assetsProvider: Provider[RemoteAssets],
                                  val environment: Environment,
                                  val eventService: MSTeamsEventService,
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
                                           activityType: String,
                                           id: String,
                                           timestamp: String,
                                           serviceUrl: String,
                                           channelId: String,
                                           from: MessageParticipantInfo,
                                           conversation: ConversationInfo,
                                           recipient: MessageParticipantInfo,
                                           textFormat: Option[String],
                                           text: Option[String],
                                           value: Option[ActionChoice],
                                           channelData: ChannelDataInfo
                                         ) extends ActionsTriggeredInfo {

    val responseUrl: String = s"$serviceUrl/v3/conversations/${conversation.id}/activities/${id}"

    val maybeTenantId: Option[String] = channelData.tenant.map(_.id)

    def toActivityInfo: ActivityInfo = ActivityInfo(
      activityType,
      id,
      timestamp,
      serviceUrl,
      channelId,
      from,
      conversation,
      recipient,
      textFormat,
      text,
      value,
      channelData
    )

    val contextName: String = Conversation.MS_TEAMS_CONTEXT
    def findButtonLabelForNameAndValue(name: String,value: String): Option[String] = None
    def findOptionLabelForValue(value: String): Option[String] = None
    def formattedUserFor(permission: ActionPermission): String = from.name
    def inputChoiceResultFor(value: String)(implicit request: Request[AnyContent]): Future[Unit] = Future.successful({})
    def instantBackgroundResponse(responseText: String, permission: ActionPermission): Future[Option[String]] = Future.successful(Some("foo"))
    def isForDataTypeChoiceForDoneConversation: Future[Boolean] = Future.successful(false)
    def isForYesNoForDoneConversation: Future[Boolean] = Future.successful(false)
    def isIncorrectTeam(botProfile: BotProfileType): Future[Boolean] = Future.successful(false)
    def isIncorrectUserTryingDataTypeChoice: Boolean = false
    def isIncorrectUserTryingYesNo: Boolean = false
    def maybeActionListForSkillId: Option[HelpGroupSearchValue] = None
    val maybeConfirmContinueConversationResponse: Option[ConfirmContinueConversationResponse] = None
    def maybeDataTypeChoice: Option[String] = None
    def maybeHelpForSkillIdWithMaybeSearch: Option[HelpGroupSearchValue] = None
    def maybeHelpIndexAt: Option[Int] = None
    def maybeHelpRunBehaviorVersionId: Option[String] = None
    def maybeSelectedActionChoice: Option[ActionChoice] = value
    val maybeStopConversationResponse: Option[StopConversationResponse] = None
    val maybeUserIdForDataTypeChoice: Option[String] = None
    def maybeYesNoAnswer: Option[String] = None
    def onEvent(event: Event): Future[Unit] = Future.successful({})
    def processTriggerableAndActiveActionChoice(
                                                 actionChoice: ActionChoice,
                                                 maybeGroupVersion: Option[BehaviorGroupVersion],
                                                 botProfile: BotProfileType,
                                                 maybeInstantResponseTs: Option[String]
                                               ): Future[Unit] = {
      for {
        _ <- dataService.msTeamsBotProfiles.sendResultWithNewEvent(
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
          toActivityInfo,
          conversation.id,
          from.id,
          id,
          None,
          isEphemeral = false,
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
                              ): Future[Option[String]] = Future.successful(None)
    val teamIdForContext: String = maybeTenantId.get // TODO: hm
    val teamIdForUserForContext: String = teamIdForContext
    def updateActionsMessageFor(maybeResultText: Option[String], shouldRemoveActions: Boolean): Future[Unit] = Future.successful({})
    val userIdForContext: String = from.id

  }

  private val messageParticipantMapping = mapping(
    "id" -> nonEmptyText,
    "name" -> nonEmptyText
  )(MessageParticipantInfo.apply)(MessageParticipantInfo.unapply)

  private val messageActivityForm = Form(
    mapping(
      "type" -> nonEmptyText,
      "id" -> nonEmptyText,
      "timestamp" -> nonEmptyText,
      "serviceUrl" -> nonEmptyText,
      "channelId" -> nonEmptyText,
      "from" -> messageParticipantMapping,
      "conversation" -> mapping(
        "id" -> nonEmptyText,
        "conversationType" -> nonEmptyText
      )(ConversationInfo.apply)(ConversationInfo.unapply),
      "recipient" -> messageParticipantMapping,
      "textFormat" -> optional(nonEmptyText),
      "text" -> optional(nonEmptyText),
      "value" -> optional(mapping(
        "label" -> nonEmptyText,
        "actionName" -> nonEmptyText,
        "args" -> optional(seq(mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(ActionArg.apply)(ActionArg.unapply))),
        "allowOthers" -> optional(boolean),
        "allowMultipleSelections" -> optional(boolean),
        "userId" -> nonEmptyText,
        "originatingBehaviorVersionId" -> nonEmptyText,
        "quiet" -> optional(boolean)
      )(ActionChoice.apply)(ActionChoice.unapply)),
      "channelData" -> mapping(
        "clientActivityId" -> optional(nonEmptyText),
        "tenant" -> optional(mapping(
          "id" -> nonEmptyText
        )(TenantInfo.apply)(TenantInfo.unapply))
      )(ChannelDataInfo.apply)(ChannelDataInfo.unapply)
    )(MSTeamsActionsTriggeredInfo.apply)(MSTeamsActionsTriggeredInfo.unapply) verifying ("Not a valid message event", fields => fields match {
      case info => info.activityType == "message"
    })
  )

  private def maybeResultFor[T](form: Form[T], resultFn: T => Result)
                               (implicit request: Request[AnyContent]): Option[Result] = {
    form.bindFromRequest.fold(
      errors => {
        Logger.info(s"Can't process MS Teams request:\n${Json.prettyPrint(errors.errorsAsJson)}")
        None
      },
      info => Some(resultFn(info))
    )
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
            info.text.orElse(info.value.map(_.label)).getOrElse(""), // TODO: formatting
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

  private def maybeMessageResult(implicit request: Request[AnyContent]): Option[Result] = {
    maybeResultFor(messageActivityForm, messageEventResult)
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
