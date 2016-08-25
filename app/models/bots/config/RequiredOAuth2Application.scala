package models.bots.config

import json.OAuth2ApplicationData
import models.IDs
import models.accounts.{OAuth2ApplicationQueries, OAuth2Application}
import models.bots.BehaviorVersion
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class RequiredOAuth2Application(
                                      id: String,
                                      behaviorVersionId: String,
                                      application: OAuth2Application
                                      ) {

}

case class RawRequiredOAuth2Application(
                                         id: String,
                                         behaviorVersionId: String,
                                         applicationId: String
                                         )

class RequiredOAuth2ApplicationsTable(tag: Tag) extends Table[RawRequiredOAuth2Application](tag, "required_oauth2_applications") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def applicationId = column[String]("application_id")

  def * = (id, behaviorVersionId, applicationId) <> ((RawRequiredOAuth2Application.apply _).tupled, RawRequiredOAuth2Application.unapply _)
}

object RequiredOAuth2ApplicationQueries {
  val all = TableQuery[RequiredOAuth2ApplicationsTable]
  val allWithApplication = all.join(OAuth2ApplicationQueries.allWithApi).on(_.applicationId === _._1.id)

  type TupleType = (RawRequiredOAuth2Application, OAuth2ApplicationQueries.TupleType)

  def tuple2RequiredApp(tuple: TupleType): RequiredOAuth2Application = {
    val raw = tuple._1
    RequiredOAuth2Application(raw.id, raw.behaviorVersionId, OAuth2ApplicationQueries.tuple2Application(tuple._2))
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = allWithApplication.filter(_._1.behaviorVersionId === behaviorVersionId)
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behaviorVersion: BehaviorVersion): DBIO[Seq[RequiredOAuth2Application]] = {
    allForQuery(behaviorVersion.id).result.map(r => r.map(tuple2RequiredApp))
  }

  def createFor(application: OAuth2Application, behaviorVersion: BehaviorVersion): DBIO[RequiredOAuth2Application] = {
    val raw = RawRequiredOAuth2Application(IDs.next, behaviorVersion.id, application.id)
    (all += raw).map(_ => RequiredOAuth2Application(raw.id, raw.behaviorVersionId, application))
  }

  def maybeCreateFor(data: OAuth2ApplicationData, behaviorVersion: BehaviorVersion): DBIO[Option[RequiredOAuth2Application]] = {
    OAuth2ApplicationQueries.find(data.applicationId).flatMap { maybeApp =>
      maybeApp.map { app =>
        createFor(app, behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    }

  }
}
