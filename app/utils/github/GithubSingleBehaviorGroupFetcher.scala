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
       |    ref(qualifiedName:"$branch") {
       |      target {
       |        ... on Commit {
       |        	oid
       |        }
       |      }
       |    }
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
    val repoData = data \ "data" \ "repository"
    repoData \ "object" \ "entries" match {
      case JsDefined(_) => {
        GithubBehaviorGroupDataBuilder(repoName, repoData.get, team, maybeBranch, dataService).
          build.
          copyForImportableForTeam(team, maybeExistingGroup)
      }
      case _ => throw GithubResultFromDataException(s"Branch '$branch' doesn't exist for $owner/$repoName")
    }
  }

}
