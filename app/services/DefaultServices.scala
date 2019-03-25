package services

import javax.inject._
import akka.actor.ActorSystem
import com.google.inject.Provider
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.caching.CacheService
import services.ms_teams.{MSTeamsApiService, MSTeamsEventService}
import services.slack.{SlackApiService, SlackEventService}
import utils.FileMap

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
                                  fileMapProvider: Provider[FileMap],
                                  slackApiServiceProvider: Provider[SlackApiService],
                                  msTeamsApiServiceProvider: Provider[MSTeamsApiService],
                                  msTeamsEventServiceProvider: Provider[MSTeamsEventService],
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
  def fileMap: FileMap = fileMapProvider.get
  def slackApiService: SlackApiService = slackApiServiceProvider.get
  def msTeamsApiService: MSTeamsApiService = msTeamsApiServiceProvider.get
  def msTeamsEventService: MSTeamsEventService = msTeamsEventServiceProvider.get
  def eventHandler: EventHandler = eventHandlerProvider.get
}
