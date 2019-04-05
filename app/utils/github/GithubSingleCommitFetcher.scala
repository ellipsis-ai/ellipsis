package utils.github

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubSingleCommitFetcher(
                                       team: Team,
                                       owner: String,
                                       repoName: String,
                                       oid: String,
                                       maybeBranch: Option[String],
                                       maybeExistingGroup: Option[BehaviorGroupData],
                                       githubService: GithubService,
                                       services: DefaultServices,
                                       implicit val ec: ExecutionContext
                                     ) extends GithubRepoFetcher[BehaviorGroupData] {

  val token: String = config.get[String]("github.repoAccessToken")

  override val cacheKey: String = s"github_published_commit_${owner}_${repoName}_${branch}_${oid}"
  override val shouldTryCache: Boolean = maybeBranch.isEmpty

  def query = {
    s"""
      |query {
      |  repository(name:"$repoName", owner:"$owner") {
      |     object(expression:"$oid") {
      |     	... on Commit {
      |         authoredDate
      |         tree {
      |           entries {
      |             name
      |             object {
      |               ... on Blob {
      |                 text
      |               }
      |               ... on Tree {
      |                 entries {
      |                   name
      |                   object {
      |                     ... on Blob {
      |                       text
      |                     }
      |                     ... on Tree {
      |                   	    entries {
      |                         name
      |                         object {
      |                  			    ... on Blob {
      |                    			    text
      |                  			    }
      |                           ... on Tree {
      |                             entries {
      |                               name
      |                               object {
      |                                 ... on Blob {
      |                                   text
      |                                 }
      |                               }
      |                             }
      |                           }
      |                         }
      |                       }
      |                     }
      |                   }
      |                 }
      |               }
      |             }
      |           }
      |         }
      |    	  }
      |     }
      |  	}
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
        val maybeTimestamp = (repoData \ "object" \ "authoredDate").asOpt[String]
        repoData \ "object" \ "tree" \ "entries" match {
          case JsDefined(_) => {
            GithubBehaviorGroupDataBuilder((repoData \ "object" \ "tree").get, team, owner, repoName, maybeBranch, Some(oid), maybeTimestamp, dataService).
              build.
              copyForImportableForTeam(team, maybeExistingGroup)
          }
          case _ => throw GithubResultFromDataException(
            GitFetcherExceptionType.NoCommitFound,
            s"Commit with SHA '$oid' wasn't found for $owner/$repoName",
            Json.obj("repo" -> repoName, "owner" -> owner, "branch" -> branch, "sha" -> oid)
          )
        }
      }
    }

  }

}
