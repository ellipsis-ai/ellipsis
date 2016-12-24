package controllers

import javax.inject.Inject

import models.accounts.user.User
import models.behaviors.SimpleTextResult
import models.behaviors.events.EventHandler
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import services.slack.NewSlackMessageEvent
import services.{DataService, SlackEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APIController @Inject() (
                                val messagesApi: MessagesApi,
                                val configuration: Configuration,
                                val dataService: DataService,
                                val ws: WSClient,
                                val cache: CacheApi,
                                val slackService: SlackEventService,
                                val eventHandler: EventHandler)
  extends EllipsisController {

  class InvalidTokenException extends Exception

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

  private def maybeUserForApiToken(token: String): Future[Option[User]] = {
    for {
      maybeToken <- dataService.apiTokens.find(token)
      maybeValidToken <- maybeToken.map { token =>
        if (token.isValid) {
          dataService.apiTokens.use(token).map(_ => Some(token))
        } else {
          Future.successful(None)
        }
      }.getOrElse(Future.successful(None))

      maybeUser <- maybeValidToken.map { token =>
        dataService.users.find(token.userId)
      }.getOrElse(Future.successful(None))
    } yield maybeUser
  }

  def postMessage = Action.async { implicit request =>
    postMessageForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.toString))
      },
      info => {
        val eventualResult = for {
          maybeUserForApiToken <- maybeUserForApiToken(info.token)
          maybeUserForInvocationToken <- dataService.users.findForInvocationToken(info.token)
          maybeUser <- Future.successful(maybeUserForApiToken.orElse(maybeUserForInvocationToken))
          maybeTeam <- maybeUser.map { user =>
            dataService.teams.find(user.teamId)
          }.getOrElse {
            throw new InvalidTokenException()
          }
          maybeBotProfile <- maybeTeam.map { team =>
            dataService.slackBotProfiles.allFor(team).map(_.headOption)
          }.getOrElse(Future.successful(None))
          maybeSlackLinkedAccount <- maybeUser.map { user =>
            dataService.linkedAccounts.maybeForSlackFor(user)
          }.getOrElse(Future.successful(None))
          maybeSlackProfile <- maybeSlackLinkedAccount.map { slackLinkedAccount =>
            dataService.slackProfiles.find(slackLinkedAccount.loginInfo)
          }.getOrElse(Future.successful(None))
          maybeEvent <- Future.successful(
            for {
              botProfile <- maybeBotProfile
            } yield NewSlackMessageEvent(
              botProfile,
              info.channel,
              maybeSlackProfile.map(_.loginInfo.providerKey).getOrElse("api"),
              info.message,
              DateTime.now.toInstant.getMillis.toString
            )
          )
          isInvokedExternally <- Future.successful(maybeUserForApiToken.isDefined)
          result <- maybeEvent.map { event =>
            for {
              _ <- eventHandler.interruptOngoingConversationsFor(event)
              result <- eventHandler.handle(event, None).map { results =>
                results.foreach { result =>
                  if (isInvokedExternally) {
                    maybeSlackProfile.foreach { slackProfile =>
                      val introResult = SimpleTextResult(s"<@${slackProfile.loginInfo.providerKey}> asked me to say:", result.forcePrivateResponse)
                      introResult.sendIn(event, None, None)
                    }
                  }
                  result.sendIn(event, None, None)
                }
                Ok(Json.toJson(results.map(_.fullText)))
              }
            } yield result
          }.getOrElse(Future.successful(NotFound("")))
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => BadRequest("Invalid token")
        }
      }
    )

  }


}
