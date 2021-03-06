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
       |          authoredDate
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
    repoData match {
      case JsDefined(JsNull) => {
        throw GithubResultFromDataException(
          GitFetcherExceptionType.NoRepoFound,
          s"Repo `$repoName' doesn't exist for $owner",
          Json.obj("repo" -> repoName, "owner" -> owner)
        )
      }
      case _ => {
        repoData \ "object" \ "entries" match {
          case JsDefined(_) => {
            val maybeGitSHA = (repoData \ "ref" \ "target" \ "oid").asOpt[String]
            val maybeTimestamp = (repoData \ "ref" \ "target" \ "authoredDate").asOpt[String]
            GithubBehaviorGroupDataBuilder((repoData \ "object").get, team, owner, repoName, maybeBranch, maybeGitSHA, maybeTimestamp, dataService).
              build.
              copyForImportableForTeam(team, maybeExistingGroup)
          }
          case _ => throw GithubResultFromDataException(
            GitFetcherExceptionType.NoBranchFound,
            s"Branch '$branch' doesn't exist for $owner/$repoName",
            Json.obj("repo" -> repoName, "owner" -> owner, "branch" -> branch)
          )
        }
      }
    }

  }

}
