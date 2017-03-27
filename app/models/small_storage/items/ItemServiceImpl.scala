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
import org.joda.time.DateTime

class ItemServiceImpl @Inject()(
                                elasticsearch: ElasticsearchService
                               )extends ItemService {

  val indexName: String = "small_storage_items"
  val itemType: String = "item-type"

  def create_and_add(team: Team, kind: String, data: JsObject): Future[Item] = {
    val item: Item = Item(id = IDs.next, team = team, kind = kind, data = data)
    add(item)
  }

  def add(item: Item): Future[Item] = {
    elasticsearch.indexDoc(indexName, itemType, Some(item.id), Json.toJson(item)).flatMap { result =>
      result.getStatusCode match {
        case 201 => {
          val returnedDocId: String = (Json.parse(result.getResponseBody()) \ "_id").as[String]
          find(returnedDocId).map(_.getOrElse(throw new Exception("Item not found")))
        }
        case _ => throw new Exception(s"Cannot store item! Elasticsearch response: ${result.getStatusCode} | ${result.getResponseBody()}")
      }
    }
  }

  def find(itemId: String): Future[Option[Item]] = {
    elasticsearch.getDoc(indexName, itemType, itemId).map { getResult =>
      getResult.getStatusCode match {
        case 200 => Some((Json.parse(getResult.getResponseBody()) \ "_source").as[Item])
        case 404 => None
        case   _ => throw new Exception("Cannot connect to storage")
      }
    }
  }

  def remove(itemId: String): Future[Option[Item]] = {
    for {
      maybeItem <- find(itemId)
      maybeDeletedItem <- maybeItem.map { _ =>
        elasticsearch.deleteDoc(indexName, itemType, itemId).map { res =>
          res.getStatusCode() match {
            case 200 => maybeItem
            case   _ => throw new Exception("Error deleting the doc")
          }
        }
      }.getOrElse(Future.successful(None))
    } yield { maybeDeletedItem }
  }

  def allForTeam(team: Team): Future[Seq[Item]] = {

  }

  def countForTeam(team: Team): Future[Int] = {
    elasticsearch.countDocs(indexName, itemType)
  }


  private def deleteDoc(itemId: String): Future[Unit] = {
    elasticsearch.deleteDoc(indexName, itemType, itemId).map { result =>
      result.getStatusCode match {
        case 200 => Unit
        case 404 => Unit
        case   _ => throw new Execption("Failed to delete item")
      }
    }
  }

  implicit val teamWrites: Writes[Team] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "timeZone").writeNullable[String]
  )(unlift(Team.unapply))

//  implicit val teamWrites: Writes[Team] = {
//    def writes(team: Team) = Json.obj(
//      "id" -> team.id,
//      "name" -> team.name,
//      "timeZone" -> team.getOrElse()
//    )
//  }

  implicit val teamReads: Reads[Team] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "timeZone").readNullable[String]
    )(Team.apply _)

  implicit val itemWrites: Writes[Item] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "kind").write[String] and
      (JsPath \ "createdAt").write[DateTime] and
      (JsPath \ "updatedAt").write[DateTime] and
      (JsPath \ "team").write[Team] and
      (JsPath \ "data").write[JsValue]
    )(unlift(Item.unapply))

  implicit val itemReads = Json.reads[Item]

}
