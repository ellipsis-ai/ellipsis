package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.events.ms_teams.MSTeamsMessageEvent
import models.behaviors.{ActionArg, ActionChoice}
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
                                ) extends EllipsisController {

  val dataService = services.dataService
  val configuration = services.configuration
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  val ws = services.ws
  val botResultService = services.botResultService
  implicit val actorSystem = services.actorSystem

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
      "locale" -> optional(nonEmptyText),
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
    )(ActivityInfo.apply)(ActivityInfo.unapply) verifying ("Not a valid message event", fields => fields match {
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

  private def processMessageEventsFor(info: ActivityInfo, botProfile: MSTeamsBotProfile)(implicit request: Request[AnyContent]): Future[Unit] = {
    for {
        // TODO: in the Slack case we check here if the user is allowed to invoke the bot. I think most of the reasons
        // don't apply in MS Teams, but we should check at some point what happens with e.g. messages from other bots
        result <- eventService.onEvent(
          MSTeamsMessageEvent(
            MSTeamsEventContext(
              botProfile,
              info
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

  private def processTriggerableAndActiveActionChoice(
                                                       actionChoice: ActionChoice,
                                                       botProfile: MSTeamsBotProfile,
                                                       info: ActivityInfo
                                                     ): Future[Unit] = {
    for {
//      maybeThreadIdToUse <- info.maybeOriginalMessageThreadId.map(tid => Future.successful(Some(tid))).getOrElse {
//        dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId).map { maybeOriginatingBehaviorVersion =>
//          if (maybeOriginatingBehaviorVersion.exists(_.responseType == Threaded)) {
//            maybeInstantResponseTs.orElse(info.original_message.map(_.ts))
//          } else {
//            None
//          }
//        }
//      }
      maybeOriginatingBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId)
      maybeGroupVersion <- Future.successful(maybeOriginatingBehaviorVersion.map(_.groupVersion))
      maybeActiveGroupVersion <- maybeGroupVersion.map { groupVersion =>
        dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(groupVersion.group, Conversation.MS_TEAMS_CONTEXT, info.conversation.id)
      }.getOrElse(Future.successful(None))
      _ <- dataService.msTeamsBotProfiles.sendResultWithNewEvent(
        s"run action named ${actionChoice.actionName}",
        event => for {
          maybeBehaviorVersion <- maybeActiveGroupVersion.map { groupVersion =>
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
        info,
        info.conversation.id,
        info.from.id,
        info.id,
        None,
        false,
        actionChoice.shouldBeQuiet
      )
    } yield {}
  }



  private def messageEventResult(info: ActivityInfo)(implicit request: Request[AnyContent]): Result = {
    for {
      maybeProfile <- info.maybeTenantId.map(id => dataService.msTeamsBotProfiles.find(id)).getOrElse(Future.successful(None))
      _ <- maybeProfile.map { profile =>
        info.value.map { actionChoice =>
          processTriggerableAndActiveActionChoice(actionChoice, profile, info)
        }.getOrElse {
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
