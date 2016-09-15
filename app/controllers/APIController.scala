package controllers

import javax.inject.Inject

import models.behaviors.SimpleTextResult
import models.behaviors.events.{APIMessageContext, APIMessageEvent, EventHandler}
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import services.{DataService, SlackService}

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
        val eventualResult = for {
          maybeToken <- dataService.apiTokens.find(info.token)
          maybeUser <- maybeToken.map { token =>
            dataService.users.find(token.userId)
          }.getOrElse(Future.successful(None))
          _ <- maybeToken.map { token =>
            if (token.isValid) {
              dataService.apiTokens.use(token).map(_ => true)
            } else {
              Future.successful(false)
            }
          }.getOrElse(Future.successful(false)).map { shouldProceed =>
            if (!shouldProceed) {
              throw new InvalidAPITokenException()
            }
          }
          maybeTeam <- maybeUser.map { user =>
            dataService.teams.find(user.teamId)
          }.getOrElse(Future.successful(None))
          maybeBotProfile <- maybeTeam.map { team =>
            dataService.slackBotProfiles.allFor(team).map(_.headOption)
          }.getOrElse(Future.successful(None))
          maybeSlackClient <- Future.successful(maybeBotProfile.flatMap { botProfile =>
            slackService.clients.get(botProfile)
          })
          maybeSlackLinkedAccount <- maybeUser.map { user =>
            dataService.linkedAccounts.maybeForSlackFor(user)
          }.getOrElse(Future.successful(None))
          maybeSlackProfile <- maybeSlackLinkedAccount.map { slackLinkedAccount =>
            dataService.slackProfiles.find(slackLinkedAccount.loginInfo)
          }.getOrElse(Future.successful(None))
          maybeEvent <- Future.successful(for {
            slackClient <- maybeSlackClient
            botProfile <- maybeBotProfile
          } yield {
              APIMessageEvent(APIMessageContext(slackClient, botProfile, info.channel, info.message))
            })
          result <- maybeEvent.map { event =>
            eventHandler.handle(event).map { result =>
              maybeSlackProfile.foreach { slackProfile =>
                val introResult = SimpleTextResult(s"<@${slackProfile.loginInfo.providerKey}> asked me to say:")
                introResult.sendIn(event.context)
              }
              result.sendIn(event.context)
              Ok(result.fullText)
            }
          }.getOrElse(Future.successful(NotFound("")))
        } yield result

        eventualResult.recover {
          case e: InvalidAPITokenException => BadRequest("Invalid API token")
        }
      }
    )

  }


}
