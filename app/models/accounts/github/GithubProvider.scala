package models.accounts.github

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers._
import models.accounts.github.profile.{GithubProfile, GithubProfileBuilder, GithubProfileParser}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future

class GithubProvider(protected val httpLayer: HTTPLayer,
                    protected val stateHandler: SocialStateHandler,
                    val settings: OAuth2Settings) extends OAuth2Provider with GithubProfileBuilder {

  import GithubProvider._

  override type Self = GithubProvider
  override type Content = JsValue

  def id = GithubProvider.ID

  override val profileParser = new GithubProfileParser

  override def withSettings(f: (Settings) => Settings) = new GithubProvider(httpLayer, stateHandler, f(settings))

  protected def urls: Map[String, String] = Map("user" -> USER_API)

  override protected val headers: Seq[(String, String)] = Seq(("Accept", "application/json"))

  protected def buildProfile(authInfo: A): Future[GithubProfile] = {
    httpLayer.url(urls("user").format(authInfo.accessToken)).get().flatMap { response =>
      println(Json.prettyPrint(response.json))
      profileParser.parse(response.json, authInfo)
    }
  }
}


object GithubProvider {
  val ID = "github"
  val USER_API = "https://api.github.com/user?access_token=%s"

  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s"
}
