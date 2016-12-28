package services

import javax.inject.Inject

import com.ning.http.client.Response
import play.api.Configuration
import models.small_storage.items.Item
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import wabisabi._

/**
  * Created by matteo on 12/8/16.
  */
class ElasticsearchServiceImpl @Inject()(
                                          val configuration: Configuration
                                        ) extends ElasticsearchService
{
  val url: String = configuration.getString("elasticsearch.url").get
  val client: wabisabi.Client = new wabisabi.Client(url)

  def createIndex(name: String, schema: String): Future[Response] = {
    return client.createIndex(name, Some(schema))
  }

  def createIndexFromFile(fileName: String): Future[Response] = {
    Future {
      Source.fromFile("conf/elasticsearch/indexes/" + fileName + ".json").getLines.mkString
    }.map { s =>
      return createIndex(fileName, s)
    }
  }

  def index(indexName: String, item: Item): Future[Response] = {
    client.index(
      indexName,
      `type` = "foo", id = Some("foo"),
      data = "{\"foo\":\"bar\"}",
      refresh = true
    )
  }
}
