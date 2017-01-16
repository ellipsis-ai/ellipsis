package services

import javax.inject.Inject
import com.ning.http.client.Response
import play.api.Configuration
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source


class ElasticsearchServiceImpl @Inject()(
                                          val configuration: Configuration
                                        ) extends ElasticsearchService
{
  val url: String = configuration.getString("elasticsearch.url").get
  val client: wabisabi.Client = new wabisabi.Client(url)

  def createIndex(name: String, schema: String): Future[Response] = {
    return client.createIndex(name, Some(schema))
  }

  def createIndexFromFile(name: String, schemaFileFullpath: String): Future[Response] = {
    Future {
      Source.fromFile(schemaFileFullpath).getLines.mkString
    }.flatMap { s =>
      createIndex(name, s)
    }
  }

  def deleteIndex(name: String): Future[Response] = {
    client.deleteIndex(name)
  }

  def deleteAllIndexes(): Future[Response] = {
    deleteIndex("*")
  }

  def verifyIndex(name: String): Future[Response] = {
    client.verifyIndex(name)
  }

  def indexDoc(indexName: String, docType: String, docId: Option[String] = None, json: JsValue): Future[Response] = {
    val indexFuture: Future[Response] = client.index(indexName, `type` = docType, id = docId, data = json.toString(), refresh = true)
    indexFuture.flatMap { indexResponse =>
      if (indexResponse.getStatusCode() == 200) {
        val docId = (Json.parse(indexResponse.getResponseBody()) \ "_id").as[String]
        client.get(indexName, docType, docId)
      } else
        indexFuture
    }
  }
  def getDoc(indexName: String, docType: String, id: String): Future[Response] = {
    client.get(indexName, docType, id)
  }

}
