package services

import javax.inject._

import akka.actor.ActorSystem
import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResultService
import models.behaviors.events.{EventHandler, SlackMessageEvent}
import play.api.Logger
import play.api.i18n.MessagesApi
import slack.api.{ApiError, SlackApiClient}
import utils.SlackMessageReactionHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class SlackEventService @Inject()(
                                   val dataService: DataService,
                                   val cacheService: CacheService,
                                   messages: MessagesApi,
                                   val eventHandler: EventHandler,
                                   val botResultService: BotResultService,
                                   implicit val actorSystem: ActorSystem
                                 ) {

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val random = new Random()

  def cacheKeyForSlackUserList(botProfile: SlackBotProfile): String = {
    s"slack-user-list-${botProfile.slackTeamId}"
  }

  def onEvent(event: SlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      val eventuallyHandleMessage = for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          Future.sequence(
            results.map(result => botResultService.sendIn(result, None).map { _ =>
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

  def maybeSlackUserListFor(botProfile: SlackBotProfile): Future[Option[Seq[SlackUserInfo]]] = {
    cacheService.getSlackUserList(cacheKeyForSlackUserList(botProfile)).map { cachedList =>
      Future.successful(Some(cachedList))
    }.getOrElse {
      for {
      // TODO: We should use pagination for fetching the list of users as per the Slack API docs
      // https://github.com/ellipsis-ai/ellipsis/issues/1720
      // https://api.slack.com/methods/users.list
        maybeUsers <- clientFor(botProfile).listUsers().map { slackUsers =>
          val slackUserList = slackUsers.map { user => SlackUserInfo(user.id, user.name, user.tz) }
          cacheService.cacheSlackUserList(cacheKeyForSlackUserList(botProfile), slackUserList)
          Some(slackUserList)
        }.recover {
          // TODO: we shouldn't swallow these errors -- instead figure out what should happen if we need a user list
          // but we can't get one
          case e: ApiError => None
        }
      } yield maybeUsers
    }
  }
}
