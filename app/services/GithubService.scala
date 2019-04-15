package services

import javax.inject.{Inject, Singleton}

import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._

import scala.concurrent.{ExecutionContext, Future}

case class GithubApiException(message: String) extends Exception

@Singleton
class GithubService @Inject()(services: DefaultServices, implicit val ec: ExecutionContext) {

  val API_URL = "https://api.github.com/graphql"

  def execute(repoAccessToken: String, query: JsValue): Future[JsValue] = {
    services.ws.url(API_URL).
      withHttpHeaders(("Authorization", s"bearer $repoAccessToken")).
      post(query).
      map { response =>
        if (response.status == 200) {
          Json.parse(response.body)
        } else {
          throw GithubApiException(s"Github API request failed with ${response.status}: ${response.body}")
        }
      }
  }

}
