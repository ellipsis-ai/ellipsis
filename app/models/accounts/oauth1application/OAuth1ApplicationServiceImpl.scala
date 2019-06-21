package models.accounts.oauth1application

import javax.inject.{Inject, Provider}

import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

case class RawOAuth1Application(
                                 id: String,
                                 name: String,
                                 apiId: String,
                                 consumerKey: String,
                                 consumerSecret: String,
                                 maybeScope: Option[String],
                                 teamId: String,
                                 isShared: Boolean,
                                 maybeSharedTokenUserId: Option[String]
                               )

class OAuth1ApplicationsTable(tag: Tag) extends Table[RawOAuth1Application](tag, "oauth1_applications") {
  def id = column[String]("id")
  def name = column[String]("name")
  def apiId = column[String]("api_id")
  def consumerKey = column[String]("consumer_key")
  def consumerSecret = column[String]("consumer_secret")
  def maybeScope = column[Option[String]]("scope")
  def teamId = column[String]("team_id")
  def isShared = column[Boolean]("is_shared")
  def maybeSharedTokenUserId = column[Option[String]]("shared_token_user_id")

  def * = (id, name, apiId, consumerKey, consumerSecret, maybeScope, teamId, isShared, maybeSharedTokenUserId) <>
    ((RawOAuth1Application.apply _).tupled, RawOAuth1Application.unapply _)

}

class OAuth1ApplicationServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService],
                                               implicit val ec: ExecutionContext
                                             ) extends OAuth1ApplicationService {

  import OAuth1ApplicationQueries._

  def dataService = dataServiceProvider.get

  def find(id: String): Future[Option[OAuth1Application]] = {
    dataService.run(findQuery(id).result.map(_.headOption.map(tuple2Application)))
  }

  def allEditableFor(team: Team): Future[Seq[OAuth1Application]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Application)
    }
    dataService.run(action)
  }

  def allUsableFor(team: Team): Future[Seq[OAuth1Application]] = {
    val action = allUsableForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Application)
    }
    dataService.run(action)
  }

  def save(application: OAuth1Application): Future[OAuth1Application] = {
    val raw = application.toRaw
    val action = findQuery(application.id).result.flatMap { r =>
      r.headOption.map { existing =>
        all.filter(_.id === application.id).update(raw)
      }.getOrElse {
        all += raw
      }
    }.map( _ => application )
    dataService.run(action)
  }
}
