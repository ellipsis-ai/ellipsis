package services

import javax.inject._

import akka.actor.ActorSystem
import com.google.inject.Provider
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient

@Singleton
class DefaultServices @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  lambdaServiceProvider: Provider[AWSLambdaService],
                                  graphQLServiceProvider: Provider[GraphQLService],
                                  cacheProvider: Provider[CacheApi],
                                  wsProvider: Provider[WSClient],
                                  configurationProvider: Provider[Configuration],
                                  val actorSystem: ActorSystem
                          ) {

  def cache: CacheApi = cacheProvider.get
  def dataService: DataService = dataServiceProvider.get
  def lambdaService: AWSLambdaService = lambdaServiceProvider.get
  def graphQLService: GraphQLService = graphQLServiceProvider.get
  def configuration: Configuration = configurationProvider.get
  def ws: WSClient = wsProvider.get
}
