package modules

import com.google.inject.Provides
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.mohiva.play.silhouette.api.crypto.Signer
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaSigner, JcaSignerSettings}
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.state.{CsrfStateItemHandler, CsrfStateSettings}
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.accounts.slack.SlackProvider
import models.silhouette._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient
import utils.CustomSecuredErrorHandler

import scala.concurrent.ExecutionContext

trait AbstractSilhouetteModule extends ScalaModule {

  def configure() {
    bind[Silhouette[EllipsisEnv]].to[SilhouetteProvider[EllipsisEnv]]
    //    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())

    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAO]
  }

  @Provides
  def provideSecureRandomIDGenerator(implicit ec: ExecutionContext): IDGenerator = new SecureRandomIDGenerator()

  @Provides
  def provideHTTPLayer(client: WSClient)(implicit ec: ExecutionContext): HTTPLayer = new PlayHTTPLayer(client)

  @Provides
  def provideSocialProviderRegistry(
                                     slackProvider: SlackProvider
                                   ): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(
      slackProvider
    ))
  }

  @Provides
  def provideAuthInfoRepository(oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info])(implicit ec: ExecutionContext): AuthInfoRepository = {

    new DelegableAuthInfoRepository(oauth2InfoDAO)
  }

  @Provides
  def provideSocialStateSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.socialStateHandler.signer")

    new JcaSigner(config)
  }

  @Provides
  def provideCsrfStateItemHandler(
                                   idGenerator: IDGenerator,
                                   signer: Signer,
                                   configuration: Configuration): CsrfStateItemHandler = {
    val settings = configuration.underlying.as[CsrfStateSettings]("silhouette.csrfStateItemHandler")
    new CsrfStateItemHandler(settings, idGenerator, signer)
  }

  @Provides
  def provideSocialStateHandler(
                                 signer: Signer,
                                 csrfStateItemHandler: CsrfStateItemHandler
                               ): SocialStateHandler = {

    // TODO: consider using state param
    new DefaultSocialStateHandler(Set(/*csrfStateItemHandler*/), signer)
  }

  @Provides
  def provideSlackProvider(
                            httpLayer: HTTPLayer,
                            stateHandler: SocialStateHandler,
                            configuration: Configuration): SlackProvider = {

    new SlackProvider(httpLayer, stateHandler, configuration.underlying.as[OAuth2Settings]("silhouette.slack"))
  }
}
