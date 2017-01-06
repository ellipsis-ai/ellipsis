package models.small_storage.items

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.IDs
import models.team.Team
import play.api.libs.json._
import services.ElasticsearchService


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
    elasticsearch.indexDoc(`indexName` = indexName, docType = itemType, json = Json.toJson(item)).map { result =>
      if (result.getStatusCode == 200)
//        Json.parse(result.getResponseBody()).as[Item]
        item
      else
//      throw!
        item
    }
  }

  implicit val itemWrites = new Writes[Item] {
    def writes(item: Item) = Json.obj(
      "id" -> item.id,
      "kind" -> item.kind,
      "data" -> item.data.asOpt[JsObject]
    )
  }

//  implicit val teamWrites: Writes[Team] = (
//      (__ \ "id").write[String] and
//      (__ \ "name").write[String]
//    ) (unlift(Team.unapply))

//  implicit val itemReads: Reads[Item] = (
//    (JsPath \ "id").read[String] and
//      (JsPath \ "long").read[Double]
//    )(Item.apply _)

}
