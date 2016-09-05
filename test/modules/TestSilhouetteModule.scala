package modules

import javax.inject.Named

import com.google.inject.Provides
import com.mohiva.play.silhouette.api.crypto.CookieSigner
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.test.FakeCookieAuthenticatorService

import scala.util.Success

trait TestSilhouetteModule extends AbstractSilhouetteModule {

  @Provides
  def provideAuthenticatorService(): AuthenticatorService[CookieAuthenticator] = FakeCookieAuthenticatorService()

  @Provides @Named("authenticator-cookie-signer")
  def provideAuthenticatorCookieSigner(): CookieSigner = {
    new CookieSigner {
      def sign(data: String) = data
      def extract(message: String) = Success(message)
    }
  }

}
