package services.ms_teams

import akka.actor.ActorSystem
import javax.inject._
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.BotResultService
import models.behaviors.events.{Event, EventHandler}
import play.api.Logger
import play.api.i18n.MessagesApi
import services.DataService
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}

class MSTeamsEventServiceImpl @Inject()(
                                       val dataService: DataService,
                                       val cacheService: CacheService,
                                       messages: MessagesApi,
                                       val eventHandler: EventHandler,
                                       val botResultService: BotResultService,
                                       val apiService: MSTeamsApiService,
                                       implicit val actorSystem: ActorSystem
                                     ) extends MSTeamsEventService {

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  def onEvent(event: Event): Future[Unit] = {
    if (!event.isBotMessage) {
      for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          Future.sequence(
            results.map(result => botResultService.sendIn(result, None).map { _ =>
              Logger.info(event.logTextFor(result, None))
            })
          )
        }
      } yield {}
    } else {
      Future.successful({})
    }
  }

  def clientFor(botProfile: MSTeamsBotProfile): MSTeamsApiClient = apiService.profileClientFor(botProfile)

}
