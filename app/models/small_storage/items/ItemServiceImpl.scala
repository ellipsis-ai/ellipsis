package models.small_storage.items

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.IDs
import models.team.Team
import services.ElasticsearchService


/**
  * Created by matteo on 12/7/16.
  */
class ItemServiceImpl @Inject()(
                                elasticsearch: ElasticsearchService
                               )extends ItemService {

  val index_name: String = "small_storage_items"

  def create(team: Team, kind: String, data: String): Future[Boolean] = {
    save(Item(id=IDs.next, team=team, kind=kind, data=data))
  }

  def save(item: Item): Future[Boolean] = {
    elasticsearch.index(index_name, item).map { result =>
      val code: Int = result.getStatusCode
      if (code == 200)
        return Future.successful(true)
      else
        return Future.successful(false)
    }
  }
}
