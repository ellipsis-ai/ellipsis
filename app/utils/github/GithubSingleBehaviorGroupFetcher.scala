package utils.github

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

case class GithubSingleBehaviorGroupFetchException(errors: Seq[String]) extends Exception {
  override def getMessage: String = errors.mkString(", ")
}

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

  def fetch(maybeExistingGroup: Option[BehaviorGroupData]): Try[BehaviorGroupData] = {
    try {
      val data = get

      val errors = data \ "errors"
      errors match {
        case JsDefined(JsArray(arr)) => throw GithubSingleBehaviorGroupFetchException(arr.map(ea => (ea \ "message").as[String]))
        case _ => {
          val obj = data \ "data" \ "repository"
          obj match {
            case JsDefined(v) => {
              Success(GithubBehaviorGroupDataBuilder(repoName, v, team, maybeBranch, dataService).
                build.
                copyForImportableForTeam(team, maybeExistingGroup))
            }
            case _ => throw new GithubSingleBehaviorGroupFetchException(Seq("Could not create a new skill version"))
          }
        }
      }
    } catch {
      case e: GithubSingleBehaviorGroupFetchException => Failure(e)
    }
  }

}
