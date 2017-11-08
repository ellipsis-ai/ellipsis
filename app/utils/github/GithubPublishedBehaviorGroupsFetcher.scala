package utils.github

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubPublishedBehaviorGroupsFetcher(
                                                  team: Team,
                                                  maybeBranch: Option[String],
                                                  githubService: GithubService,
                                                  services: DefaultServices,
                                                  implicit val ec: ExecutionContext
                                                ) extends GithubFetcher {

  val owner: String = "ellipsis-ai"
  val repoName: String = "behaviors"
  val repoAccessToken: String = config.get[String]("github.repoAccessToken")

  override val cacheKey: String = s"github_published_behaviors_${branch}"

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

  def publishedBehaviorGroupsFor(alreadyInstalled: Seq[BehaviorGroupData]): Seq[BehaviorGroupData] = {
    val data = get
    val behaviorGroups = (data \ "data" \ "repository" \ "object" \ "entries") match {
      case JsDefined(JsArray(arr)) => {
        arr.map { ea =>
          val groupPath = (ea \ "name").as[String]
          GithubBehaviorGroupDataBuilder(groupPath, ea, team, maybeBranch, dataService).build
        }
      }
      case _ => Seq()
    }
    behaviorGroups.map { ea =>
      val maybeExistingGroup = alreadyInstalled.find(_.exportId == ea.exportId)
      ea.copyForImportableForTeam(team, maybeExistingGroup)
    }.sorted
  }

}
