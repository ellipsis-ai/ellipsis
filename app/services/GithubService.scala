package services

import json.EditorFormat.BehaviorVersionData
import models.Team
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class GithubService(team: Team, ws: WSClient, config: Configuration) {

  val repoCredentials: (String, String) = ("access_token", config.getString("GITHUB_REPO_TOKEN").get)

  private def withTreeFor(url: String): Future[Option[Seq[JsValue]]] = {
    ws.url(url).withQueryString(repoCredentials).get().map { response =>
      val json = Json.parse(response.body)
      (json \ "tree").asOpt[Seq[JsValue]]
    }
  }

  private def fetchPublishedUrl: Future[Option[String]] = {
    withTreeFor(s"${GithubService.API_URL}/repos/ellipsis-ai/behaviors/git/trees/master").map { maybeTree =>
      for {
        tree <- maybeTree
        published <- tree.find { ea => (ea \ "path").asOpt[String].contains("published") }
        url <- (published \ "url").asOpt[String]
      } yield url
    }
  }

  private def fetchBehaviorUrlsFor(publishedUrl: String): Future[Seq[String]] = {
    withTreeFor(publishedUrl).map { maybeTree =>
      maybeTree.map { tree =>
        tree.flatMap { item =>
          (item \ "url").asOpt[String]
        }
      }.getOrElse(Seq())
    }
  }

  case class BehaviorCode(functionUrl: String, responseUrl: String, triggersUrl: String, paramsUrl: String) {

    private def fetchTextFor(url: String): Future[String] = {
      ws.url(url).
        withQueryString(repoCredentials).
        withHeaders(("Accept", "application/vnd.github.v3.raw")).
        get().
        map { response =>
          response.body
        }
    }

    def fetchData: Future[BehaviorVersionData] = {
      for {
        function <- fetchTextFor(functionUrl)
        response <- fetchTextFor(responseUrl)
        params <- fetchTextFor(paramsUrl)
        triggers <- fetchTextFor(triggersUrl)
      } yield BehaviorVersionData.fromStrings(
        team.id,
        function,
        response,
        params,
        triggers
      )
    }

  }

  private def urlForTreeFileNamed(name: String, inTree: Seq[JsValue]): Option[String] = {
    inTree.find { ea => (ea \ "path").asOpt[String].contains(name) }.flatMap { jsValue =>
      (jsValue \ "url").asOpt[String]
    }
  }

  private def fetchBehaviorCodeFor(behaviorUrl: String): Future[Option[BehaviorCode]] = {
    withTreeFor(behaviorUrl).map { maybeTree =>
      for {
        tree <- maybeTree
        functionUrl <- urlForTreeFileNamed("function.js", tree)
        responseUrl <- urlForTreeFileNamed("response.md", tree)
        triggersUrl <- urlForTreeFileNamed("triggers.json", tree)
        paramsUrl <- urlForTreeFileNamed("params.json", tree)
      } yield BehaviorCode(functionUrl, responseUrl, triggersUrl, paramsUrl)
    }
  }

  def fetchPublishedBehaviors: Future[Seq[BehaviorVersionData]] = {
    for {
      maybePublishedUrl <- fetchPublishedUrl
      behaviorUrls <- maybePublishedUrl.map { publishedUrl =>
        fetchBehaviorUrlsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      behaviorCodes <- Future.sequence(behaviorUrls.map(fetchBehaviorCodeFor)).map(_.flatten)
      behaviorData <- Future.sequence(behaviorCodes.map(_.fetchData))
    } yield behaviorData
  }

}

object GithubService {

  val API_URL = "https://api.github.com"
}
