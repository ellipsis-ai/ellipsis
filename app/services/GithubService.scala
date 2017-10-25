package services

import javax.inject.{Inject, Singleton}

import json.BehaviorGroupData
import models.team.Team
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import utils.GithubDataBuilder

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class GithubService @Inject()(
                                ws: WSClient,
                                config: Configuration,
                                cacheService: CacheService,
                                dataService: DataService,
                                implicit val ec: ExecutionContext
                              ) {

  val API_URL = "https://api.github.com/graphql"
  val USER_NAME = "ellipsis-ai"
  val REPO_NAME = "behaviors"

  val PUBLISHED_BEHAVIORS_KEY = "github_published_behaviors"

  val repoAccessToken: String = config.get[String]("github.repoAccessToken")
  val cacheTimeout: Duration = config.get[Int]("github.cacheTimeoutSeconds").seconds

  def branchFor(maybeBranch: Option[String]) = maybeBranch.getOrElse("master")

  def queryFor(branch: String): String = {
    s"""
       |query {
       |  repository(name:"$REPO_NAME", owner:"$USER_NAME") {
       |    object(expression:"$branch:published") {
       |      ... on Tree {
       |        entries {
       |          name
       |          object {
       |            ... on Blob {
       |              text
       |            }
       |            ... on Tree {
       |              entries {
       |                name
       |                object {
       |                  ... on Blob {
       |                    text
       |                  }
       |                  ... on Tree {
       |                   	entries {
       |                      name
       |                      object {
       |                  			... on Blob {
       |                    			text
       |                  			}
       |                        ... on Tree {
       |                          entries {
       |                            name
       |                            object {
       |                              ... on Blob {
       |                                text
       |                              }
       |                            }
       |                          }
       |                        }
       |                      }
       |                    }
       |                  }
       |                }
       |              }
       |            }
       |          }
       |        }
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  private def jsonQueryFor(branch: String): JsValue = {
    JsObject(Seq(("query", JsString(queryFor(branch)))))
  }

  def fetchDataFor(branch: String): Future[JsValue] = {
    ws.url(API_URL).
      withHttpHeaders(("Authorization", s"bearer $repoAccessToken")).
      post(jsonQueryFor(branch)).
      map { response =>
        Json.parse(response.body)
      }
  }

  def fetchPublishedBehaviorGroups(team: Team, maybeBranch: Option[String]): Future[Seq[BehaviorGroupData]] = {
    for {
      data <- fetchDataFor(branchFor(maybeBranch))
      groupData <- Future.successful({
        (data \ "data" \ "repository" \ "object" \ "entries") match {
          case JsDefined(JsArray(arr)) => {
            arr.map { ea =>
              GithubDataBuilder(ea, team, maybeBranch, dataService).build
            }
          }
          case _ => Seq()
        }
      })
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
