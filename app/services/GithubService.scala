package services

import java.time.OffsetDateTime

import json._
import json.Formatting._
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class GithubService(team: Team, ws: WSClient, config: Configuration, cache: CacheApi, dataService: DataService, maybeBranch: Option[String]) {

  import GithubService._

  val repoCredentials: (String, String) = ("access_token", config.getString("github.repoAccessToken").get)
  val cacheTimeout: Duration = config.getInt("github.cacheTimeoutSeconds").get.seconds

  val shouldTryCache: Boolean = maybeBranch.isEmpty
  val branch = maybeBranch.getOrElse("master")

  private def withTreeFor(url: String): Future[Option[Seq[JsValue]]] = {
    ws.url(url).withQueryString(repoCredentials).get().map { response =>
      val json = Json.parse(response.body)
      (json \ "tree").asOpt[Seq[JsValue]]
    }
  }

  private def fetchPublishedUrl: Future[Option[String]] = {
    withTreeFor(s"${API_URL}/repos/ellipsis-ai/behaviors/git/trees/$branch").map { maybeTree =>
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
                           maybeDescriptionUrl: Option[String],
                           functionUrl: String,
                           responseUrl: String,
                           triggersUrl: String,
                           paramsUrl: String
                           ) {

    def fetchData: Future[BehaviorVersionData] = {
      for {
        config <- fetchTextFor(configUrl)
        description <- maybeDescriptionUrl.map { url =>
          fetchTextFor(url).map(Some(_))
        }.getOrElse(Future.successful(None))
        function <- fetchTextFor(functionUrl)
        response <- fetchTextFor(responseUrl)
        params <- fetchTextFor(paramsUrl)
        triggers <- fetchTextFor(triggersUrl)
      } yield BehaviorVersionData.fromStrings(
        team.id,
        description,
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

  private def githubUrlForGroupPath(groupPath: String): String = {
    s"${WEB_URL}/${USER_NAME}/${REPO_NAME}/tree/$branch/published/$groupPath"
  }

  private def githubUrlForBehaviorPath(categoryPath: String, behaviorType: String, behaviorPath: String): String = {
    s"${githubUrlForGroupPath(categoryPath)}/$behaviorType/$behaviorPath"
  }

  private def fetchBehaviorDataFor(behaviorUrl: String, behaviorPath: String, behaviorType: String, categoryPath: String): Future[Option[BehaviorVersionData]] = {
    withTreeFor(behaviorUrl).flatMap { maybeTree =>
      (for {
        tree <- maybeTree
        configUrl <- urlForTreeFileNamed("config.json", tree)
        functionUrl <- urlForTreeFileNamed("function.js", tree)
        responseUrl <- urlForTreeFileNamed("response.md", tree)
        triggersUrl <- urlForTreeFileNamed("triggers.json", tree)
        paramsUrl <- urlForTreeFileNamed("params.json", tree)
      } yield {
        val githubUrl = githubUrlForBehaviorPath(categoryPath, behaviorType, behaviorPath)
        val maybeDescriptionUrl = urlForTreeFileNamed("README", tree)
        BehaviorCode(githubUrl, configUrl, maybeDescriptionUrl, functionUrl, responseUrl, triggersUrl, paramsUrl).fetchData.map(Some(_))
      }).getOrElse(Future.successful(None))
    }
  }

  private def fetchGroupConfigFor(configUrl: String): Future[Option[BehaviorGroupConfig]] = {
    fetchTextFor(configUrl).map { configText =>
      Json.parse(configText).validate[BehaviorGroupConfig] match {
        case JsSuccess(data, jsPath) => Some(data)
        case e: JsError => None
      }
    }
  }

  private def fetchInputsFor(url: String): Future[Seq[InputData]] = {
    fetchTextFor(url).map { inputsText =>
      Json.parse(inputsText).validate[Seq[InputData]] match {
        case JsSuccess(data, jsPath) => data
        case e: JsError => Seq()
      }
    }
  }

  private def fetchGroupDataFor(groupUrl: String, groupPath: String): Future[Option[BehaviorGroupData]] = {
    withTreeFor(groupUrl).flatMap { maybeTree =>
      (for {
        tree <- maybeTree
        readmeUrl <- urlForTreeFileNamed("README", tree)
        configUrl <- urlForTreeFileNamed("config.json", tree)
        inputsUrl <- urlForTreeFileNamed("inputs.json", tree)
      } yield {
          (for {
            readme <- fetchTextFor(readmeUrl)
            maybeConfig <- fetchGroupConfigFor(configUrl)
            inputs <- fetchInputsFor(inputsUrl)
            behaviors <- fetchBehaviorsFor(groupUrl, groupPath)
          } yield {
            val githubUrl = githubUrlForGroupPath(groupPath)
            val maybePublishedId = maybeConfig.map(_.publishedId)
            val name = maybeConfig.map(_.name).getOrElse(groupPath)
            val icon = maybeConfig.flatMap(_.icon)
            BehaviorGroupData(None, name, readme, icon, inputs, behaviors, Some(githubUrl), None, maybePublishedId, OffsetDateTime.now)
          }).map(Some(_))
        }).getOrElse(Future.successful(None))
    }
  }

  def fetchBehaviorsFor(categoryUrl: String, categoryPath: String): Future[Seq[BehaviorVersionData]] = {
    for {
      urls <- fetchTreeUrlsFor(categoryUrl)
      paths <- fetchPathsFor(categoryUrl)
      behaviorData <- {
        val eventualBehaviorData: Seq[Future[Seq[BehaviorVersionData]]] = urls.zip(paths).map { case (url, path) =>
          path match {
            case "data_types" | "actions" => {
              for {
                behaviorUrls <- fetchTreeUrlsFor(url)
                behaviorPaths <- fetchPathsFor(url)
                data <- Future.sequence(behaviorUrls.zip(behaviorPaths).map { case (behaviorUrl, behaviorPath) =>
                  fetchBehaviorDataFor(behaviorUrl, behaviorPath, path, categoryPath)
                }).map(_.flatten)
              } yield data
            }
            case _ => Future.successful(Seq())
          }
        }
        Future.sequence(eventualBehaviorData).map(_.flatten)
      }
    } yield behaviorData
  }

  def fetchPublishedBehaviorGroups: Future[Seq[BehaviorGroupData]] = {
    for {
      maybePublishedUrl <- fetchPublishedUrl
      groupUrls <- maybePublishedUrl.map { publishedUrl =>
        fetchTreeUrlsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      groupPaths <- maybePublishedUrl.map { publishedUrl =>
        fetchPathsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      groupData <- {
        val eventualGroupData = groupUrls.zip(groupPaths).map((fetchGroupDataFor _).tupled)
        Future.sequence(eventualGroupData).map(_.flatten)
      }
    } yield groupData
  }

  def blockingFetchPublishedBehaviorGroups: Seq[BehaviorGroupData] = {
    Await.result(fetchPublishedBehaviorGroups, 20.seconds)
  }

  def publishedBehaviorGroups: Seq[BehaviorGroupData] = {
    val behaviorGroups = if (shouldTryCache) {
      cache.getOrElse[Seq[BehaviorGroupData]](PUBLISHED_BEHAVIORS_KEY, cacheTimeout) {
        blockingFetchPublishedBehaviorGroups
      }
    } else {
      blockingFetchPublishedBehaviorGroups
    }
    behaviorGroups.map(_.copyForTeam(team)).sorted
  }

}

object GithubService {

  val API_URL = "https://api.github.com"
  val WEB_URL = "https://github.com"
  val USER_NAME = "ellipsis-ai"
  val REPO_NAME = "behaviors"

  val PUBLISHED_BEHAVIORS_KEY = "github_published_behaviors"
}
