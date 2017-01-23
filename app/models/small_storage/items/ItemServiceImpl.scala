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
    elasticsearch.indexDoc(indexName, itemType, Json.toJson(item)).flatMap { result =>
      result.getStatusCode match {
        case 201 => {
          val returnedDocId: String = (Json.parse(result.getResponseBody()) \ "_id").as[String]
          findById(returnedDocId).map {
            case Some(Item) => Some(Item)
            case None => throw new Exception("Item not found")
          }
        }
        case _ => throw new Exception(s"Cannot store item! Elasticsearch response: ${result.getStatusCode} | ${result.getResponseBody()}")
      }
    }
  }

  def findById(itemId: String): Future[Option[Item]] = {
    elasticsearch.getDoc(indexName, itemType, itemId).map { getResult =>
      getResult.getStatusCode match {
        case 200 => Some((Json.parse(getResult.getResponseBody()) \ "_source").as[Item])
        case 404 => None
        case   _ => throw new Exception("Cannot connect to storage")
      }
    }
  }

  def deleteDoc(itemId: String): Future[Unit] = {
    elasticsearch.deleteDoc(indexName, itemType, itemId).map { result =>
      result.getStatusCode match {
        case 200 or 404 => Unit
        case   _ => throw new Execption("Failed to delete item")
      }
    }
  }

//  def delete(itemId: String): Future[Option[Item]] = {
//    val maybeItem = findById(itemId)
//    if maybeItem
//      val deleteDocResult = elasticsearch.deleteDoc(`indexName` = indexName, docType = itemType, id = itemId)
//      if deleteDocResult == 200
//        Item
//      else
//        throw new Exection("Failed to delete item")
//    else
//      None
//    for {
//      maybeItem <- findById(itemId)
//      _ <- deleteItem(itemId) if maybeItem.isDefined
//    } yield { maybeItem }
//
//    findById(itemId).flatMap {
//      case Some(Item) => deleteItem(itemId).map { Future(Item) }
//      case None => Future(None)
//    }
//
//  }

  def allForTeam(team: Team): Future[Seq[Item]] = {

  }

  def count(team: Team): Future[Int] = {
    elasticsearch.count(indexName = indexName, docType = itemType)
  }

  implicit val teamReads = Json.reads[Team]
  implicit val teamWrites = Json.writes[Team]
  implicit val itemWrites = Json.writes[Item]
  implicit val itemReads = Json.reads[Item]

}
