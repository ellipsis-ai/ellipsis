package controllers

import javax.inject.Inject

import models.{APITokenQueries, Models, Team}
import models.accounts._
import models.bots.events.{APIMessageContext, APIMessageEvent, EventHandler}
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

  class InvalidAPITokenException extends Exception

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
          _ <- maybeTeam.map { team =>
            APITokenQueries.find(info.token, team).flatMap { maybeToken =>
              maybeToken.map { token =>
                if (token.isValid) {
                  APITokenQueries.use(token, team).map(_ => true)
                } else {
                  DBIO.successful(false)
                }
              }.getOrElse(DBIO.successful(false)).map { shouldProceed =>
                if (!shouldProceed) {
                  throw new InvalidAPITokenException()
                }
              }
            }
          }.getOrElse(DBIO.successful(Unit))
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
            DBIO.from(eventHandler.handle(event)).map { result =>
              Ok(result.fullText)
            }
          }.getOrElse(DBIO.successful(NotFound("")))
        } yield result

        models.run(action).recover {
          case e: InvalidAPITokenException => BadRequest("Invalid API token")
        }
      }
    )

  }


}
