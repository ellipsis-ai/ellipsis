package services

import scala.concurrent.Future
import play.api.Configuration
import com.ning.http.client.Response
import play.api.libs.json.{JsObject, JsValue}

trait ElasticsearchService {

  val client: wabisabi.Client
  val configuration: Configuration

  def createIndex(name: String, schema: String): Future[Response]

  def createIndexFromFile(name: String, schemaFileFullpath: String): Future[Response]

  def indexDoc(index: String, docType: String, docId: Option[String] = None, json: JsValue): Future[Response]

  def deleteIndex(name: String): Future[Response]

  def deleteAllIndexes(): Future[Response]

  def verifyIndex(name: String): Future[Response]

  def getDoc(index: String, docType: String, id: String): Future[Response]

  def deleteDoc(index: String, docType: String, id: String): Future[Response]

  def countDocs(index: String, docType: String): Future[Int]

  def search(index: String, query: JsObject)
}
