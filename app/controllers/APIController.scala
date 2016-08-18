package controllers

import javax.inject.Inject

import models.bots._
import models.{Team, Models}
import models.accounts._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import services.SlackService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIController @Inject() (
                                val messagesApi: MessagesApi,
                                val configuration: Configuration,
                                val models: Models,
                                val ws: WSClient,
                                val cache: CacheApi,
                                val slackService: SlackService,
                                val eventHandler: EventHandler)
  extends Controller {

  case class PostMessageInfo(
                              message: String,
                              teamId: String,
                              responseContext: String,
                              channel: String,
                              token: String
                              )

  private val postMessageForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "teamId" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(PostMessageInfo.apply)(PostMessageInfo.unapply)
  )

  def postMessage = Action.async { implicit request =>
    postMessageForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        val action = for {
          maybeTeam <- Team.find(info.teamId)
          maybeBotProfile <- maybeTeam.map { team =>
            SlackBotProfileQueries.allFor(team).map(_.headOption)
          }.getOrElse(DBIO.successful(None))
          maybeSlackClient <- DBIO.successful(maybeBotProfile.flatMap { botProfile =>
            slackService.clients.get(botProfile)
          })
          maybeEvent <- DBIO.successful(for {
            slackClient <- maybeSlackClient
            botProfile <- maybeBotProfile
          } yield {
              APIMessageEvent(APIMessageContext(slackClient, botProfile, info.channel, info.message))
            })
          result <- maybeEvent.map { event =>
            DBIO.from(eventHandler.handle(event)).map { _ =>
              Ok("success")
            }
          }.getOrElse(DBIO.successful(NotFound("")))
        } yield result

        models.run(action)
      }
    )

  }


}
