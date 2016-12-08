package models.small_storage.items


import javax.inject.{Named, Inject}

import com.google.inject.Provider
import models.IDs
import models.team.Team
import play.api.Configuration
import services.DataService
import scala.concurrent.{ExecutionContext, Future}

import com.sksamuel.elastic4s.{IndexAndType, ElasticDsl}
import com.evojam.play.elastic4s.configuration.ClusterSetup
import com.evojam.play.elastic4s.{PlayElasticFactory, PlayElasticJsonSupport}


import scala.concurrent.Future

/**
  * Created by matteo on 12/7/16.
  */
class ItemServiceImpl @Inject() (
                                  cs: ClusterSetup,
                                  elasticFactory: PlayElasticFactory,
                                  @Named("items") indexAndType: IndexAndType
                                ) extends ElasticDsl with PlayElasticJsonSupport with ItemService {

  private[this] lazy val client = elasticFactory(cs)

  def create(team: Team, kind: String, data: String): Future[Item] = {
    save(Item(id=IDs.next, team=team, kind=kind, data=data))
  }

  def save(item: Item): Future[Item] = {
    Future.successful[Item](item)
  }

}
