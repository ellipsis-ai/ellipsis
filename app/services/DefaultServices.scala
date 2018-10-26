package services

import javax.inject._
import akka.actor.ActorSystem
import com.google.inject.Provider
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.caching.CacheService
import services.slack.{SlackApiService, SlackEventService}
import utils.SlackFileMap

@Singleton
class DefaultServices @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  lambdaServiceProvider: Provider[AWSLambdaService],
                                  graphQLServiceProvider: Provider[GraphQLService],
                                  cacheServiceProvider: Provider[CacheService],
                                  wsProvider: Provider[WSClient],
                                  configurationProvider: Provider[Configuration],
                                  botResultServiceProvider: Provider[BotResultService],
                                  slackEventServiceProvider: Provider[SlackEventService],
                                  slackFileMapProvider: Provider[SlackFileMap],
                                  slackApiServiceProvider: Provider[SlackApiService],
                                  eventHandlerProvider: Provider[EventHandler],
                                  val actorSystem: ActorSystem
                          ) {

  def cacheService: CacheService = cacheServiceProvider.get
  def dataService: DataService = dataServiceProvider.get
  def lambdaService: AWSLambdaService = lambdaServiceProvider.get
  def graphQLService: GraphQLService = graphQLServiceProvider.get
  def configuration: Configuration = configurationProvider.get
  def ws: WSClient = wsProvider.get
  def botResultService: BotResultService = botResultServiceProvider.get
  def slackEventService: SlackEventService = slackEventServiceProvider.get
  def slackFileMap: SlackFileMap = slackFileMapProvider.get
  def slackApiService: SlackApiService = slackApiServiceProvider.get
  def eventHandler: EventHandler = eventHandlerProvider.get
}
