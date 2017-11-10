package utils.github

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubSingleBehaviorGroupFetcher(
                                             team: Team,
                                             owner: String,
                                             repoName: String,
                                             token: String,
                                             maybeBranch: Option[String],
                                             maybeExistingGroup: Option[BehaviorGroupData],
                                             githubService: GithubService,
                                             services: DefaultServices,
                                             implicit val ec: ExecutionContext
                                           ) extends GithubRepoFetcher[BehaviorGroupData] {

  def query: String = {
    s"""
       |query {
       |  repository(name:"$repoName", owner:"$owner") {
       |    object(expression:"$branch:") {
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

  def resultFromNonErrorResponse(data: JsValue): BehaviorGroupData = {
    val obj = data \ "data" \ "repository"
    obj match {
      case JsDefined(v) => {
        GithubBehaviorGroupDataBuilder(repoName, v, team, maybeBranch, dataService).
          build.
          copyForImportableForTeam(team, maybeExistingGroup)
      }
      case _ => throw GithubResultFromDataException("Could not build a skill from response")
    }
  }

}
