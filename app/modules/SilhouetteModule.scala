package modules

import javax.inject.Named

import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.mohiva.play.silhouette.api.crypto.{CookieSigner, Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.crypto.{JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.accounts.user.{UserService, UserServiceImpl}
import models.accounts.SlackProvider
import models.silhouette._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import utils.CustomSecuredErrorHandler

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[Silhouette[EllipsisEnv]].to[SilhouetteProvider[EllipsisEnv]]
//    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[UserService].to[UserServiceImpl]
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
  def provideEnvironment(
                          userService: UserService,
                          authenticatorService: AuthenticatorService[CookieAuthenticator],
                          eventBus: EventBus): Environment[EllipsisEnv] = {

    Environment[EllipsisEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideSocialProviderRegistry(
                                     slackProvider: SlackProvider
                                     ): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(
      slackProvider
    ))
  }

  @Provides
  def provideAuthenticatorService(
                                   @Named("authenticator-cookie-signer") cookieSigner: CookieSigner,
                                   @Named("authenticator-crypter") crypter: Crypter,
                                   fingerprintGenerator: FingerprintGenerator,
                                   idGenerator: IDGenerator,
                                   configuration: Configuration,
                                   clock: Clock
                                 ): AuthenticatorService[CookieAuthenticator] = {

    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(config, None, cookieSigner, encoder, fingerprintGenerator, idGenerator, clock)
  }

  @Provides @Named("authenticator-cookie-signer")
  def provideAuthenticatorCookieSigner(configuration: Configuration): CookieSigner = {
    val config = configuration.underlying.as[JcaCookieSignerSettings]("silhouette.authenticator.cookie.signer")

    new JcaCookieSigner(config)
  }

  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
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
