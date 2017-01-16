package services

import scala.concurrent.Future
import play.api.Configuration
import com.ning.http.client.Response
import play.api.libs.json.JsValue

trait ElasticsearchService {

  val client: wabisabi.Client
  val configuration: Configuration

  def createIndex(name: String, schema: String): Future[Response]

  def createIndexFromFile(name: String, schemaFileFullpath: String): Future[Response]

  def indexDoc(indexName: String, docType: String, docId: Option[String] = None, json: JsValue): Future[Response]

  def deleteIndex(name: String): Future[Response]

  def deleteAllIndexes(): Future[Response]

  def verifyIndex(name: String): Future[Response]

  def getDoc(indexName: String, docType: String, id: String): Future[Response]
}
