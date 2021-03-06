package utils.github

import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services._
import services.caching.CacheService

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

sealed trait GitFetcherException extends Exception

object GitFetcherExceptionType extends Enumeration {
  type GitFetcherExceptionType = Value
  val NoCommiterInfoFound, NoRepoFound, NoBranchFound, NoCommitFound, NoValidSkillFound = Value
}

case class GithubResultFromDataException(exceptionType: GitFetcherExceptionType.Value, message: String, details: JsObject) extends GitFetcherException {
  override def getMessage: String = message
}

case class GithubFetchDataException(errors: Seq[String]) extends GitFetcherException {
  override def getMessage: String = errors.mkString(", ")
}

trait GithubFetcher[T] {

  val token: String
  val githubService: GithubService
  val services: DefaultServices
  val ws: WSClient = services.ws
  val config: Configuration = services.configuration
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService
  implicit val ec: ExecutionContext

  val cacheKey: String
  val cacheTimeout: Duration = config.get[Int]("github.cacheTimeoutSeconds").seconds
  val shouldTryCache: Boolean = false

  def query: String

  protected def jsonQuery: JsValue = {
    JsObject(Seq(("query", JsString(query))))
  }

  def fetch: Future[JsValue] = {
    githubService.execute(token, jsonQuery)
  }

  def blockingFetch: JsValue = {
    Await.result(fetch, 20.seconds)
  }

  def get: Future[JsValue] = {
    if (shouldTryCache) {
      cacheService.getJsonReadable[JsValue](cacheKey).flatMap { maybeValue =>
        maybeValue.map(v => Future.successful(v)).getOrElse {
          try {
            fetch.flatMap { fetched =>
              if (isCacheable(fetched)) {
                cacheService.set(cacheKey, cacheService.toJsonString(fetched), cacheTimeout).map(_ => fetched)
              } else {
                Future.successful(fetched)
              }
            }
          } catch {
            case ex: GithubApiException => Future.successful(JsArray())
          }
        }
      }
    } else {
      fetch
    }
  }

  def resultFromResponse(data: JsValue): T = {
    val errors = data \ "errors"
    errors match {
      case JsDefined(JsArray(arr)) => throw GithubFetchDataException(arr.map(ea => (ea \ "message").as[String]))
      case _ => resultFromNonErrorResponse(data)
    }
  }

  def resultFromNonErrorResponse(data: JsValue): T

  def isCacheable(data: JsValue): Boolean = {
    try {
      resultFromResponse(data)
      true
    } catch {
      case _: GitFetcherException => false
    }
  }

  def result: Future[T] = get.map(resultFromNonErrorResponse)

}
