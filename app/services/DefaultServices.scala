package services

import javax.inject._

import akka.actor.ActorSystem
import com.google.inject.Provider
import models.behaviors.BotResultService
import play.api.Configuration
import play.api.libs.ws.WSClient

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
                                  val actorSystem: ActorSystem
                          ) {

  def cacheService: CacheService = cacheServiceProvider.get
  def dataService: DataService = dataServiceProvider.get
  def lambdaService: AWSLambdaService = lambdaServiceProvider.get
  def graphQLService: GraphQLService = graphQLServiceProvider.get
  def configuration: Configuration = configurationProvider.get
  def ws: WSClient = wsProvider.get
  def botResultService = botResultServiceProvider.get
  def slackEventService = slackEventServiceProvider.get
}
