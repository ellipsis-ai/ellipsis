package models.accounts

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import play.api.libs.json.JsValue

import scala.concurrent.Future

class CustomProvider(
                     val configuration: CustomOAuth2Configuration,
                     val httpLayer: HTTPLayer
                     ) extends OAuth2Provider {

  val settings: OAuth2Settings = configuration.oAuth2Settings
  val stateProvider: OAuth2StateProvider = new DummyStateProvider

  override type Self = CustomProvider
  override type Content = JsValue
  type Profile = CustomProfile

  def id = configuration.name

  override val profileParser = new CustomProfileParser(configuration)

  override protected val headers: Seq[(String, String)] = Seq(("Accept", "application/json"))

  override def withSettings(f: (Settings) => Settings) = new CustomProvider(configuration, httpLayer)

  protected def urls: Map[String, String] = Map("getProfile" -> configuration.getProfileUrl)

  protected def buildProfile(authInfo: A): Future[CustomProfile] = {
    httpLayer.url(urls("getProfile").format(authInfo.accessToken)).
      withHeaders(headers: _*).
      get().
      flatMap { response =>
        profileParser.parse(response.json)
      }
  }

}
