package services

import javax.inject._

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.{EventHandler, SlackMessageEvent}
import play.api.Logger
import play.api.i18n.MessagesApi
import slack.api.SlackApiClient
import utils.SlackMessageReactionHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class SlackEventService @Inject()(
                                   val dataService: DataService,
                                   messages: MessagesApi,
                                   val eventHandler: EventHandler,
                                   implicit val actorSystem: ActorSystem
                                 ) {

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val random = new Random()

  def onEvent(event: SlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      val eventuallyHandleMessage = for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          Future.sequence(
            results.map(result => result.sendIn(None, dataService).map { _ =>
              Logger.info(event.logTextFor(result))
            })
          )
        }
      } yield {}
      SlackMessageReactionHandler.handle(event.client, eventuallyHandleMessage, event.channel, event.ts)
    } else {
      Future.successful({})
    }
  }

  def clientFor(botProfile: SlackBotProfile): SlackApiClient = SlackApiClient(botProfile.token)

}
