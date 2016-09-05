package modules

import com.google.inject.Provides
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.accounts.SlackProvider
import models.silhouette._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import utils.CustomSecuredErrorHandler

trait AbstractSilhouetteModule extends ScalaModule {

  def configure() {
    bind[Silhouette[EllipsisEnv]].to[SilhouetteProvider[EllipsisEnv]]
    //    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())

    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAO]
  }

  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  @Provides
  def provideSocialProviderRegistry(
                                     slackProvider: SlackProvider
                                   ): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(
      slackProvider
    ))
  }

  @Provides
  def provideAuthInfoRepository(oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info]): AuthInfoRepository = {

    new DelegableAuthInfoRepository(oauth2InfoDAO)
  }

  @Provides
  def provideSlackProvider(
                            httpLayer: HTTPLayer,
                            configuration: Configuration): SlackProvider = {

    new SlackProvider(httpLayer, new DummyStateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.slack"))
  }
}
