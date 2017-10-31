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

  def fetch(maybeExistingGroup: Option[BehaviorGroupData]): Either[Seq[String], BehaviorGroupData] = {
    try {
      val data = get

      val errors = data \ "errors"
      errors match {
        case JsDefined(JsArray(arr)) => Left(arr.map(ea => (ea \ "message").as[String]))
        case _ => {
          val obj = data \ "data" \ "repository"
          obj match {
            case JsDefined(v) => {
              val group =
                GithubBehaviorGroupDataBuilder(repoName, v, team, maybeBranch, dataService).
                  build.
                  copyForImportableForTeam(team, maybeExistingGroup)
              Right(group)
            }
            case _ => Left(Seq("Can't do it"))
          }
        }
      }
    } catch {
      case e: Exception => Left(Seq(e.getMessage))
    }
  }

}
