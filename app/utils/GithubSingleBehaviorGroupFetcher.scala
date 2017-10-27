package utils

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubSingleBehaviorGroupFetcher(
                                             team: Team,
                                             owner: String,
                                             repoName: String,
                                             repoAccessToken: String,
                                             maybeBranch: Option[String],
                                             githubService: GithubService,
                                             services: DefaultServices,
                                             implicit val ec: ExecutionContext
                                           ) extends GithubFetcher {

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

  def maybeBehaviorGroup(maybeExistingGroup: Option[BehaviorGroupData]): Option[BehaviorGroupData] = {
    val data = get
    val obj = data \ "data" \ "repository"
    val maybeGroup = obj match {
      case JsDefined(v) => {
        Some(GithubBehaviorGroupDataBuilder(repoName, v, team, maybeBranch, dataService).build)
      }
      case _ => None
    }
    maybeGroup.map { group =>
      group.copyForImportableForTeam(team, maybeExistingGroup)
    }
  }

}
