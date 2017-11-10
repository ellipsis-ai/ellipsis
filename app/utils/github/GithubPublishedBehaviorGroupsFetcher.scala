package utils.github

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubPublishedBehaviorGroupsFetcher(
                                                  team: Team,
                                                  maybeBranch: Option[String],
                                                  alreadyInstalled: Seq[BehaviorGroupData],
                                                  githubService: GithubService,
                                                  services: DefaultServices,
                                                  implicit val ec: ExecutionContext
                                                ) extends GithubRepoFetcher[Seq[BehaviorGroupData]] {

  val owner: String = "ellipsis-ai"
  val repoName: String = "behaviors"
  val token: String = config.get[String]("github.repoAccessToken")

  override val cacheKey: String = s"github_published_behaviors_${branch}"
  override val shouldTryCache: Boolean = maybeBranch.isEmpty

  def query: String = {
    s"""
       |query {
       |  repository(name:"$repoName", owner:"$owner") {
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

  def resultFromNonErrorResponse(data: JsValue): Seq[BehaviorGroupData] = {
    val behaviorGroups = (data \ "data" \ "repository" \ "object" \ "entries") match {
      case JsDefined(JsArray(arr)) => {
        arr.map { ea =>
          val groupPath = (ea \ "name").as[String]
          GithubBehaviorGroupDataBuilder(groupPath, ea, team, maybeBranch, dataService).build
        }
      }
      case _ => throw GithubResultFromDataException("Could not build skills from response")
    }
    behaviorGroups.map { ea =>
      val maybeExistingGroup = alreadyInstalled.find(_.exportId == ea.exportId)
      ea.copyForImportableForTeam(team, maybeExistingGroup)
    }.sorted
  }

}
