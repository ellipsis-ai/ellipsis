package utils

import models.team.Team
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait GithubFetcher {

  val owner: String
  val repoName: String
  val repoAccessToken: String
  val team: Team
  val maybeBranch: Option[String]
  val githubService: GithubService
  val services: DefaultServices
  val ws: WSClient = services.ws
  val config: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService
  implicit val ec: ExecutionContext

  val API_URL = "https://api.github.com/graphql"

  val cacheKey: String = s"github_${owner}_${repoName}_${branch}"

  val cacheTimeout: Duration = config.get[Int]("github.cacheTimeoutSeconds").seconds

  val branch: String = maybeBranch.getOrElse("master")

  def query: String

  private def jsonQuery: JsValue = {
    JsObject(Seq(("query", JsString(query))))
  }

  def fetch: Future[JsValue] = {
    githubService.execute(repoAccessToken, jsonQuery)
  }

  def blockingFetch: JsValue = {
    Await.result(fetch, 20.seconds)
  }

  def get: JsValue = {
    val shouldTryCache = maybeBranch.isEmpty
    if (shouldTryCache) {
      cacheService.get(cacheKey).getOrElse {
        try {
          val fetched = blockingFetch
          cacheService.set(cacheKey, fetched, cacheTimeout)
          fetched
        } catch {
          case ex: GithubApiException => JsArray()
        }
      }
    } else {
      blockingFetch
    }
  }

}
