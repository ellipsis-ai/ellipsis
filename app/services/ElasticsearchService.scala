package services

import com.ning.http.client.Response

import scala.concurrent.Future
import models.small_storage.items.Item
import play.api.Configuration

trait ElasticsearchService {

  val client: wabisabi.Client
  val configuration: Configuration

  def createIndex(name: String, schema: String): Future[Response]

  def createIndexFromFile(fileName: String): Future[Response]

  def index(indexName: String, item: Item): Future[Response]

}
