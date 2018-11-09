package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.events._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
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
      "textFormat" -> nonEmptyText,
      "locale" -> optional(nonEmptyText),
      "text" -> nonEmptyText,
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
        // TODO: take a look
//      isUserValidForBot <- slackEventService.isUserValidForBot(info.userId, botProfile, info.maybeEnterpriseId)
//      result <- if (!isUserValidForBot) {
//        if (info.channel.startsWith("D") || botProfile.includesBotMention(slackMessage)) {
//          dataService.teams.find(botProfile.teamId).flatMap { maybeTeam =>
//            val teamText = maybeTeam.map { team =>
//              s"the ${team.name} team"
//            }.getOrElse("another team")
//            sendEphemeralMessage(s"Sorry, I'm only able to respond to people from ${teamText}.", info)
//          }
//        } else {
//          Future.successful({})
//        }
//      } else {
//        val maybeFile = info.event match {
//          case e: MessageSentEventInfo => e.maybeFilesInfo.flatMap(_.headOption.map(i => SlackFile(i.downloadUrl, i.maybeThumbnailUrl)))
//          case _ => None
//        }
        result <- eventService.onEvent(
          MSTeamsMessageEvent(
            MSTeamsEventContext(
              botProfile,
              info
            ),
            info.text, // TODO: formatting
            None,
            isUninterruptedConversation = false,
            isEphemeral = false,
            None,
            beQuiet = false
          )
        )
    } yield result
  }


  private def messageEventResult(info: ActivityInfo)(implicit request: Request[AnyContent]): Result = {
    for {
      maybeProfile <- info.maybeTenantId.map(id => dataService.msTeamsBotProfiles.find(id)).getOrElse(Future.successful(None))
      _ <- maybeProfile.map { profile =>
        processMessageEventsFor(info, profile)
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
