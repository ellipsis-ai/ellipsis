package services

import json._
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class GithubService(team: Team, ws: WSClient, config: Configuration, cache: CacheApi, dataService: DataService) {

  import GithubService._

  val repoCredentials: (String, String) = ("access_token", config.getString("github.repoAccessToken").get)
  val cacheTimeout: Duration = config.getInt("github.cacheTimeoutSeconds").get.seconds

  private def withTreeFor(url: String): Future[Option[Seq[JsValue]]] = {
    ws.url(url).withQueryString(repoCredentials).get().map { response =>
      val json = Json.parse(response.body)
      (json \ "tree").asOpt[Seq[JsValue]]
    }
  }

  private def fetchPublishedUrl: Future[Option[String]] = {
    withTreeFor(s"${API_URL}/repos/ellipsis-ai/behaviors/git/trees/master").map { maybeTree =>
      for {
        tree <- maybeTree
        published <- tree.find { ea => (ea \ "path").asOpt[String].contains("published") }
        url <- (published \ "url").asOpt[String]
      } yield url
    }
  }

  private def fetchPropertyFrom(property: String, treeUrl: String): Future[Seq[String]] = {
    withTreeFor(treeUrl).map { maybeTree =>
      maybeTree.map { tree =>
        tree.flatMap { item =>
          (item \ property).asOpt[String]
        }
      }.getOrElse(Seq())
    }
  }

  private def fetchTreeUrlsFor(treeUrl: String): Future[Seq[String]] = fetchPropertyFrom("url", treeUrl)
  private def fetchPathsFor(treeUrl: String): Future[Seq[String]] = fetchPropertyFrom("path", treeUrl)

  private def fetchTextFor(url: String): Future[String] = {
    ws.url(url).
      withQueryString(repoCredentials).
      withHeaders(("Accept", "application/vnd.github.v3.raw")).
      get().
      map { response =>
      response.body
    }
  }

  case class BehaviorCode(
                           githubUrl: String,
                           configUrl: String,
                           functionUrl: String,
                           responseUrl: String,
                           triggersUrl: String,
                           paramsUrl: String
                           ) {

    def fetchData: Future[BehaviorVersionData] = {
      for {
        config <- fetchTextFor(configUrl)
        function <- fetchTextFor(functionUrl)
        response <- fetchTextFor(responseUrl)
        params <- fetchTextFor(paramsUrl)
        triggers <- fetchTextFor(triggersUrl)
      } yield BehaviorVersionData.fromStrings(
        team.id,
        function,
        response,
        params,
        triggers,
        config,
        Some(githubUrl),
        dataService
      )
    }

  }

  private def urlForTreeFileNamed(name: String, inTree: Seq[JsValue]): Option[String] = {
    inTree.find { ea => (ea \ "path").asOpt[String].contains(name) }.flatMap { jsValue =>
      (jsValue \ "url").asOpt[String]
    }
  }

  private def githubUrlForBehaviorPath(categoryPath: String, behaviorPath: String): String = {
    s"${WEB_URL}/${USER_NAME}/${REPO_NAME}/tree/master/published/$categoryPath/$behaviorPath"
  }

  private def fetchBehaviorDataFor(behaviorUrl: String, behaviorPath: String, categoryPath: String): Future[Option[BehaviorVersionData]] = {
    withTreeFor(behaviorUrl).flatMap { maybeTree =>
      (for {
        tree <- maybeTree
        configUrl <- urlForTreeFileNamed("config.json", tree)
        functionUrl <- urlForTreeFileNamed("function.js", tree)
        responseUrl <- urlForTreeFileNamed("response.md", tree)
        triggersUrl <- urlForTreeFileNamed("triggers.json", tree)
        paramsUrl <- urlForTreeFileNamed("params.json", tree)
      } yield {
          val githubUrl = githubUrlForBehaviorPath(categoryPath, behaviorPath)
          BehaviorCode(githubUrl, configUrl, functionUrl, responseUrl, triggersUrl, paramsUrl).fetchData.map(Some(_))
        }).getOrElse(Future.successful(None))
    }
  }

  private def fetchCategoryDataFor(categoryUrl: String, categoryPath: String): Future[Option[BehaviorCategory]] = {
    withTreeFor(categoryUrl).flatMap { maybeTree =>
      (for {
        tree <- maybeTree
        readmeUrl <- urlForTreeFileNamed("README", tree)
      } yield {
          (for {
            readme <- fetchTextFor(readmeUrl)
            behaviors <- fetchBehaviorsFor(categoryUrl, categoryPath)
          } yield {
            BehaviorCategory(categoryPath, readme, behaviors)
          }).map(Some(_))
        }).getOrElse(Future.successful(None))
    }
  }

  def fetchBehaviorsFor(categoryUrl: String, categoryPath: String): Future[Seq[BehaviorVersionData]] = {
    for {
      behaviorUrls <- fetchTreeUrlsFor(categoryUrl)
      behaviorPaths <- fetchPathsFor(categoryUrl)
      behaviorData <- {
        val eventualBehaviorData = behaviorUrls.zip(behaviorPaths).map { case (url, path) =>
          fetchBehaviorDataFor(url, path, categoryPath)
        }
        Future.sequence(eventualBehaviorData).map(_.flatten)
      }
    } yield behaviorData
  }

  def fetchPublishedBehaviorCategories: Future[Seq[BehaviorCategory]] = {
    for {
      maybePublishedUrl <- fetchPublishedUrl
      categoryUrls <- maybePublishedUrl.map { publishedUrl =>
        fetchTreeUrlsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      categoryPaths <- maybePublishedUrl.map { publishedUrl =>
        fetchPathsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      categoryData <- {
        val eventualCategoryData = categoryUrls.zip(categoryPaths).map((fetchCategoryDataFor _).tupled)
        Future.sequence(eventualCategoryData).map(_.flatten)
      }
    } yield categoryData
  }

  def publishedBehaviorCategories: Seq[BehaviorCategory] = {
    cache.getOrElse[Seq[BehaviorCategory]](PUBLISHED_BEHAVIORS_KEY, cacheTimeout) {
      Await.result(fetchPublishedBehaviorCategories, 20.seconds)
    }.map(_.copyForTeam(team)).sorted
  }

}

object GithubService {

  val API_URL = "https://api.github.com"
  val WEB_URL = "https://github.com"
  val USER_NAME = "ellipsis-ai"
  val REPO_NAME = "behaviors"

  val PUBLISHED_BEHAVIORS_KEY = "github_published_behaviors"
}
