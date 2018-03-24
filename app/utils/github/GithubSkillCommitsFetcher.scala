package utils.github

import json._
import models.team.Team
import play.api.libs.json._
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

case class SkillCommit(owner: String, repoName: String, commitId: String)

case class SubmoduleInfo(name: String, path: String, url: String) {
  val maybeOwner: Option[String] = GithubUtils.maybeOwnerFor(url)
  val maybeRepoName: Option[String] = GithubUtils.maybeNameFor(url)
}

case class CommitInfo(name: String, commitId: String)

case class GithubSkillCommitsFetcher(
                                     team: Team,
                                     maybeBranch: Option[String],
                                     alreadyInstalled: Seq[BehaviorGroupData],
                                     githubService: GithubService,
                                     services: DefaultServices,
                                     implicit val ec: ExecutionContext
                                   ) extends GithubRepoFetcher[Seq[SkillCommit]] {

  val owner: String = "ellipsis-ai"
  val repoName: String = "skills"
  val token: String = config.get[String]("github.repoAccessToken")

  override val cacheKey: String = s"github_published_skills_${branch}"
  override val shouldTryCache: Boolean = maybeBranch.isEmpty

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
       |          }
       |          type
       |          oid
       |        }
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  private val submoduleConfigRegex: Regex = """(?s)\[\s*submodule\s+\"([^\"]+)\"\]\s*path\s*=\s*(\S+)\s*url\s*=\s*(\S+)\s*""".r

  private def submoduleInfosFor(entries: Seq[JsValue]): Seq[SubmoduleInfo] = {
    val maybeSubmodules = entries.find { ea =>
      (ea \ "name").asOpt[String].contains(".gitmodules")
    }
    val maybeSubmodulesText = maybeSubmodules.flatMap { submodules =>
      (submodules \ "object" \ "text") match {
        case JsDefined(JsString(str)) => Some(str)
        case _ => None
      }
    }
    maybeSubmodulesText.map { text =>
      submoduleConfigRegex.findAllMatchIn(text).map { ea =>
        val g = ea.subgroups
        SubmoduleInfo(g.head, g(1), g(2))
      }.toSeq
    }.getOrElse(Seq())
  }

  private def commitInfosFor(entries: Seq[JsValue]): Seq[CommitInfo] = {
    val commits = entries.filter { ea => (ea \ "type").asOpt[String].contains("commit") }
    commits.flatMap { ea =>
      for {
        name <- (ea \ "name").asOpt[String]
        commitId <- (ea \ "oid").asOpt[String]
      } yield CommitInfo(name, commitId)
    }
  }

  def resultFromNonErrorResponse(data: JsValue): Seq[SkillCommit] = {
    (data \ "data" \ "repository" \ "object" \ "entries") match {
      case JsDefined(JsArray(arr)) => {
        val submoduleInfos = submoduleInfosFor(arr)
        val commitInfos = commitInfosFor(arr)
        submoduleInfos.flatMap { ea =>
          for {
            owner <- ea.maybeOwner
            repoName <- ea.maybeRepoName
            commitId <- commitInfos.find(_.name == ea.name).map(_.commitId)
          } yield SkillCommit(owner, repoName, commitId)
        }
      }
      case _ => throw GithubResultFromDataException(
        GitFetcherExceptionType.NoValidSkillFound,
        "Could not build skills from response",
        data.asOpt[JsObject].getOrElse(Json.obj("data" -> data))
      )
    }
  }

}
