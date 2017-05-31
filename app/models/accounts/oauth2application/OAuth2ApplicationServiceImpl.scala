package models.accounts.oauth2application

import javax.inject.{Inject, Provider}

import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawOAuth2Application(
                                 id: String,
                                 name: String,
                                 apiId: String,
                                 clientId: String,
                                 clientSecret: String,
                                 maybeScope: Option[String],
                                 teamId: String,
                                 isShared: Boolean
                               )

class OAuth2ApplicationsTable(tag: Tag) extends Table[RawOAuth2Application](tag, "oauth2_applications") {
  def id = column[String]("id")
  def name = column[String]("name")
  def apiId = column[String]("api_id")
  def clientId = column[String]("client_id")
  def clientSecret = column[String]("client_secret")
  def maybeScope = column[Option[String]]("scope")
  def teamId = column[String]("team_id")
  def isShared = column[Boolean]("is_shared")

  def * = (id, name, apiId, clientId, clientSecret, maybeScope, teamId, isShared) <>
    ((RawOAuth2Application.apply _).tupled, RawOAuth2Application.unapply _)

}

class OAuth2ApplicationServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends OAuth2ApplicationService {

  import OAuth2ApplicationQueries._

  def dataService = dataServiceProvider.get

  def find(id: String): Future[Option[OAuth2Application]] = {
    dataService.run(findQuery(id).result.map(_.headOption.map(tuple2Application)))
  }

  def allEditableFor(team: Team): Future[Seq[OAuth2Application]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Application)
    }
    dataService.run(action)
  }

  def allUsableFor(team: Team): Future[Seq[OAuth2Application]] = {
    val action = allUsableForTeamQuery(team.id).result.map { r =>
      r.map(tuple2Application)
    }
    dataService.run(action)
  }

  def save(application: OAuth2Application): Future[OAuth2Application] = {
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
