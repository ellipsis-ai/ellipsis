package services

import java.nio.charset.Charset
import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}

import json.Formatting._
import json._
import models.team.Team
import org.apache.commons.codec.binary.Base64
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class GithubService @Inject() (
                                ws: WSClient,
                                config: Configuration,
                                cacheService: CacheService,
                                dataService: DataService,
                                implicit val ec: ExecutionContext
                              ) {

  val API_URL = "https://api.github.com"
  val WEB_URL = "https://github.com"
  val USER_NAME = "ellipsis-ai"
  val REPO_NAME = "behaviors"

  val PUBLISHED_BEHAVIORS_KEY = "github_published_behaviors"

  val repoCredentials: (String, String) = ("access_token", config.get[String]("github.repoAccessToken"))
  val cacheTimeout: Duration = config.get[Int]("github.cacheTimeoutSeconds").seconds

  def branchFor(maybeBranch: Option[String]) = maybeBranch.getOrElse("master")

  private def withTreeFor(url: String): Future[Option[Seq[JsValue]]] = {
    ws.url(url).withQueryString(repoCredentials).get().map { response =>
      val json = Json.parse(response.body)
      (json \ "tree").asOpt[Seq[JsValue]]
    }
  }

  private def fetchPublishedUrl(maybeBranch: Option[String]): Future[Option[String]] = {
    withTreeFor(s"${API_URL}/repos/ellipsis-ai/behaviors/git/trees/${branchFor(maybeBranch)}").map { maybeTree =>
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
      withHeaders(("Accept", "application/vnd.github.v3.json")).
      get().
      map { response =>
        (Json.parse(response.body) \ "content").validate[String].map { base64Content =>
          new String(Base64.decodeBase64(base64Content), Charset.forName("UTF-8"))
        }.getOrElse("")
      }
  }

  case class BehaviorCode(
                           team: Team,
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

  private def githubUrlForGroupPath(groupPath: String, maybeBranch: Option[String]): String = {
    s"${WEB_URL}/${USER_NAME}/${REPO_NAME}/tree/${branchFor(maybeBranch)}/published/$groupPath"
  }

  private def githubUrlForBehaviorPath(categoryPath: String, behaviorType: String, behaviorPath: String, maybeBranch: Option[String]): String = {
    s"${githubUrlForGroupPath(categoryPath, maybeBranch)}/$behaviorType/$behaviorPath"
  }

  private def fetchBehaviorDataFor(
                                    behaviorUrl: String,
                                    behaviorPath: String,
                                    behaviorType: String,
                                    categoryPath: String,
                                    team: Team,
                                    maybeBranch: Option[String]
                                  ): Future[Option[BehaviorVersionData]] = {
    withTreeFor(behaviorUrl).flatMap { maybeTree =>
      (for {
        tree <- maybeTree
        configUrl <- urlForTreeFileNamed("config.json", tree)
        functionUrl <- urlForTreeFileNamed("function.js", tree)
        responseUrl <- urlForTreeFileNamed("response.md", tree)
        triggersUrl <- urlForTreeFileNamed("triggers.json", tree)
        paramsUrl <- urlForTreeFileNamed("params.json", tree)
      } yield {
        val githubUrl = githubUrlForBehaviorPath(categoryPath, behaviorType, behaviorPath, maybeBranch)
        val maybeDescriptionUrl = urlForTreeFileNamed("README", tree)
        BehaviorCode(team, githubUrl, configUrl, maybeDescriptionUrl, functionUrl, responseUrl, triggersUrl, paramsUrl).fetchData.map(Some(_))
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

  private def fetchGroupDataFor(groupUrl: String, groupPath: String, team: Team, maybeBranch: Option[String]): Future[Option[BehaviorGroupData]] = {
    withTreeFor(groupUrl).flatMap { maybeTree =>
      (for {
        tree <- maybeTree
        readmeUrl <- urlForTreeFileNamed("README", tree)
        configUrl <- urlForTreeFileNamed("config.json", tree)
        actionInputsUrl <- urlForTreeFileNamed("action_inputs.json", tree)
        dataTypeInputsUrl <- urlForTreeFileNamed("data_type_inputs.json", tree)
      } yield {
          (for {
            readme <- fetchTextFor(readmeUrl)
            maybeConfig <- fetchGroupConfigFor(configUrl)
            actionInputs <- fetchInputsFor(actionInputsUrl)
            dataTypeInputs <- fetchInputsFor(dataTypeInputsUrl)
            behaviors <- fetchBehaviorsFor(groupUrl, groupPath, team, maybeBranch)
            libraries <- fetchLibrariesFor(groupUrl, groupPath, team, maybeBranch)
          } yield {
            val githubUrl = githubUrlForGroupPath(groupPath, maybeBranch)
            val maybeExportId = maybeConfig.flatMap(_.exportId)
            val name = maybeConfig.map(_.name).getOrElse(groupPath)
            val icon = maybeConfig.flatMap(_.icon)
            val requiredOAuth2ApiConfigData = maybeConfig.map(_.requiredOAuth2ApiConfigs).getOrElse(Seq())
            val requiredSimpleTokenApiData = maybeConfig.map(_.requiredSimpleTokenApis).getOrElse(Seq())
            BehaviorGroupData(
              None,
              team.id,
              Some(name),
              Some(readme),
              icon,
              actionInputs,
              dataTypeInputs,
              behaviors,
              libraries,
              nodeModuleVersions = Seq(),
              requiredOAuth2ApiConfigData,
              requiredSimpleTokenApiData,
              Some(githubUrl),
              maybeExportId,
              Some(OffsetDateTime.now)
            )
          }).map(Some(_))
        }).getOrElse(Future.successful(None))
    }
  }

  def fetchBehaviorsFor(categoryUrl: String, categoryPath: String, team: Team, maybeBranch: Option[String]): Future[Seq[BehaviorVersionData]] = {
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
                  fetchBehaviorDataFor(behaviorUrl, behaviorPath, path, categoryPath, team, maybeBranch)
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

  private def fetchLibraryFrom(tree: JsValue): Future[Option[LibraryVersionData]] = {
    (for {
      url <- (tree \ "url").asOpt[String]
      path <- (tree \ "path").asOpt[String]
    } yield {
      fetchTextFor(url).map { text =>
        Some(LibraryVersionData.fromFile(text, s"lib/$path"))
      }
    }).getOrElse {
      Future.successful(None)
    }
  }

  def fetchLibrariesFor(categoryUrl: String, categoryPath: String, team: Team, maybeBranch: Option[String]): Future[Seq[LibraryVersionData]] = {
    for {
      urls <- fetchTreeUrlsFor(categoryUrl)
      paths <- fetchPathsFor(categoryUrl)
      libraryData <- {
        val eventualData: Seq[Future[Seq[LibraryVersionData]]] = urls.zip(paths).map { case (url, path) =>
          path match {
            case "lib" => {
              withTreeFor(url).flatMap { maybeTree =>
                maybeTree.map { tree =>
                  Future.sequence(tree.map { ea => fetchLibraryFrom(ea) }).map(_.flatten)
                }.getOrElse(Future.successful(Seq()))
              }
            }
            case _ => Future.successful(Seq())
          }
        }
        Future.sequence(eventualData).map(_.flatten)
      }
    } yield libraryData
  }

  def fetchPublishedBehaviorGroups(team: Team, maybeBranch: Option[String]): Future[Seq[BehaviorGroupData]] = {
    for {
      maybePublishedUrl <- fetchPublishedUrl(maybeBranch)
      groupUrls <- maybePublishedUrl.map { publishedUrl =>
        fetchTreeUrlsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      groupPaths <- maybePublishedUrl.map { publishedUrl =>
        fetchPathsFor(publishedUrl)
      }.getOrElse(Future.successful(Seq()))
      groupData <- {
        val eventualGroupData = groupUrls.zip(groupPaths).map { case(url, path) =>
          fetchGroupDataFor(url, path, team, maybeBranch)
        }
        Future.sequence(eventualGroupData).map(_.flatten)
      }
    } yield groupData
  }

  def blockingFetchPublishedBehaviorGroups(team: Team, maybeBranch: Option[String]): Seq[BehaviorGroupData] = {
    Await.result(fetchPublishedBehaviorGroups(team, maybeBranch), 20.seconds)
  }

  def publishedBehaviorGroupsFor(team: Team, maybeBranch: Option[String], alreadyInstalled: Seq[BehaviorGroupData]): Seq[BehaviorGroupData] = {
    val shouldTryCache = maybeBranch.isEmpty
    val behaviorGroups = if (shouldTryCache) {
      cacheService.getBehaviorGroupData(PUBLISHED_BEHAVIORS_KEY).getOrElse {
        val fetched = blockingFetchPublishedBehaviorGroups(team, maybeBranch)
        cacheService.cacheBehaviorGroupData(PUBLISHED_BEHAVIORS_KEY, fetched, cacheTimeout)
        fetched
      }
    } else {
      blockingFetchPublishedBehaviorGroups(team, maybeBranch)
    }
    behaviorGroups.map { ea =>
      val maybeExistingGroup = alreadyInstalled.find(_.exportId == ea.exportId)
      ea.copyForImportableForTeam(team, maybeExistingGroup)
    }.sorted
  }

}
