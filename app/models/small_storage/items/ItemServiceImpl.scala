package models.small_storage.items

import javax.inject.Inject

import com.ning.http.client.Response

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.IDs
import models.team.Team
import play.api.libs.json._
import services.ElasticsearchService
import play.api.libs.functional.syntax._

class ItemServiceImpl @Inject()(
                                elasticsearch: ElasticsearchService
                               )extends ItemService {

  val indexName: String = "small_storage_items"
  val itemType: String = "item-type"

  def create(team: Team, kind: String, data: JsObject): Future[Item] = {
    val item: Item = Item(id = IDs.next, team = team, kind = kind, data = data)
    save(item)
  }


  def save(item: Item): Future[Item] = {
    val indexDocOp: Future[Response] = elasticsearch.indexDoc(`indexName` = indexName, docType = itemType, json = Json.toJson(item))

    elasticsearch.indexDoc(`indexName` = indexName, docType = itemType, json = Json.toJson(item)).flatMap { result =>
      if (result.getStatusCode == 201) {
        val resAsJson = Json.parse(result.getResponseBody())
        val returnedDocId: String = (resAsJson \ "_id").as[String]
        val returnedDocType: String = (resAsJson \ "_type").as[String]
        elasticsearch.getDoc(indexName, returnedDocType, returnedDocId).map { getResult =>
          if (getResult.getStatusCode == 200) {
            val itemAsJson: JsValue = Json.parse(getResult.getResponseBody())
            (itemAsJson \ "_source").as[Item]
          }
          else throw new Exception("Cannot retrieve new item!")
        }
      }
      else throw new Exception(s"Cannot store item! Elasticsearch response: ${result.getStatusCode} | ${result.getResponseBody()}")
    }
  }

  implicit val teamReads = Json.reads[Team]
  implicit val teamWrites = Json.writes[Team]
  implicit val itemWrites = Json.writes[Item]
  implicit val itemReads = Json.reads[Item]

}
