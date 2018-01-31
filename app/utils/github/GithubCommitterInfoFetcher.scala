package utils.github

import models.accounts.user.User
import play.api.libs.json.{JsDefined, JsObject, JsValue, Json}
import services.{DefaultServices, GithubService}

import scala.concurrent.ExecutionContext

case class GithubCommitterInfo(name: String, email: String)

case class GithubCommitterInfoFetcher(
                                       user: User,
                                       token: String,
                                       githubService: GithubService,
                                       services: DefaultServices,
                                       implicit val ec: ExecutionContext
                                     ) extends GithubFetcher[GithubCommitterInfo] {

  val cacheKey: String = s"github_committer_info_${user.id}"

  def query: String = {
    s"""
       |query {
       |  viewer {
       |    name
       |    email
       |  }
       |}
     """.stripMargin
  }

  def resultFromNonErrorResponse(data: JsValue): GithubCommitterInfo = {
    val obj = data \ "data" \ "viewer"
    obj match {
      case JsDefined(v) => {
        val name = (obj \ "name").as[String]
        val email = Option((obj \ "email").as[String]).map(_.trim).filter(_.nonEmpty).getOrElse("<>")
        GithubCommitterInfo(name, email)
      }
      case _ => {
        throw GithubResultFromDataException(
          GitFetcherExceptionType.NoCommiterInfoFound,
          "Could not fetch committer info",
          Json.obj("userId" -> user.id)
        )
      }
    }
  }

}
