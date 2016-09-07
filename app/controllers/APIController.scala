package controllers

import javax.inject.Inject

import models.APITokenQueries
import models.accounts._
import models.bots.SimpleTextResult
import models.bots.events.{APIMessageContext, APIMessageEvent, EventHandler}
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import services.{DataService, SlackService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIController @Inject() (
                                val messagesApi: MessagesApi,
                                val configuration: Configuration,
                                val dataService: DataService,
                                val ws: WSClient,
                                val cache: CacheApi,
                                val slackService: SlackService,
                                val eventHandler: EventHandler)
  extends EllipsisController {

  class InvalidAPITokenException extends Exception

  case class PostMessageInfo(
                              message: String,
                              responseContext: String,
                              channel: String,
                              token: String
                              )

  private val postMessageForm = Form(
    mapping(
      "message" -> nonEmptyText,
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
          maybeToken <- APITokenQueries.find(info.token)
          maybeUser <- maybeToken.map { token =>
            DBIO.from(dataService.users.find(token.userId))
          }.getOrElse(DBIO.successful(None))
          _ <- maybeToken.map { token =>
            if (token.isValid) {
              APITokenQueries.use(token).map(_ => true)
            } else {
              DBIO.successful(false)
            }
          }.getOrElse(DBIO.successful(false)).map { shouldProceed =>
            if (!shouldProceed) {
              throw new InvalidAPITokenException()
            }
          }
          maybeTeam <- maybeUser.map { user =>
            DBIO.from(dataService.teams.find(user.teamId))
          }.getOrElse(DBIO.successful(None))
          maybeBotProfile <- maybeTeam.map { team =>
            SlackBotProfileQueries.allFor(team).map(_.headOption)
          }.getOrElse(DBIO.successful(None))
          maybeSlackClient <- DBIO.successful(maybeBotProfile.flatMap { botProfile =>
            slackService.clients.get(botProfile)
          })
          maybeSlackLinkedAccount <- maybeUser.map { user =>
            DBIO.from(dataService.linkedAccounts.maybeForSlackFor(user))
          }.getOrElse(DBIO.successful(None))
          maybeSlackProfile <- maybeSlackLinkedAccount.map { slackLinkedAccount =>
            SlackProfileQueries.find(slackLinkedAccount.loginInfo)
          }.getOrElse(DBIO.successful(None))
          maybeEvent <- DBIO.successful(for {
            slackClient <- maybeSlackClient
            botProfile <- maybeBotProfile
          } yield {
              APIMessageEvent(APIMessageContext(slackClient, botProfile, info.channel, info.message))
            })
          result <- maybeEvent.map { event =>
            DBIO.from(eventHandler.handle(event)).map { result =>
              maybeSlackProfile.foreach { slackProfile =>
                val introResult = SimpleTextResult(s"<@${slackProfile.loginInfo.providerKey}> asked me to say:")
                introResult.sendIn(event.context)
              }
              result.sendIn(event.context)
              Ok(result.fullText)
            }
          }.getOrElse(DBIO.successful(NotFound("")))
        } yield result

        dataService.run(action).recover {
          case e: InvalidAPITokenException => BadRequest("Invalid API token")
        }
      }
    )

  }


}
