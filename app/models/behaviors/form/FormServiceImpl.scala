package models.behaviors.form

import java.time.OffsetDateTime

import javax.inject.Inject
import com.google.inject.Provider
import Formatting._
import models.IDs
import services.{AWSLambdaService, DataService}
import drivers.SlickPostgresDriver.api._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}


case class RawForm(
                    id: String,
                    config: JsValue,
                    createdFromBehaviorGroupVersionId: String,
                    createdAt: OffsetDateTime
                  )

class FormsTable(tag: Tag) extends Table[RawForm](tag, "forms") {

  def id = column[String]("id", O.PrimaryKey)
  def config = column[JsValue]("config")
  def createdFromBehaviorGroupVersionId = column[String]("created_from_group_version_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, config, createdFromBehaviorGroupVersionId, createdAt) <> ((RawForm.apply _).tupled, RawForm.unapply _)
}

class FormServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  lambdaServiceProvider: Provider[AWSLambdaService],
                                  implicit val ec: ExecutionContext
                                ) extends FormService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import FormQueries._

  def createAction(config: FormConfig, createdFromBehaviorGroupVersionId: String): DBIO[Form] = {
    val raw = RawForm(IDs.next, Json.toJson(config), createdFromBehaviorGroupVersionId, OffsetDateTime.now)
    (all += raw).map(_ => Form.fromRaw(raw))
  }

  def create(config: FormConfig, createdFromBehaviorGroupVersionId: String): Future[Form] = {
    dataService.run(createAction(config, createdFromBehaviorGroupVersionId))
  }

  def find(id: String): Future[Option[Form]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(Form.fromRaw)
    }
    dataService.run(action)
  }
}
