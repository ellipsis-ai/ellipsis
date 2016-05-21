package services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import models.Team
import play.api.Configuration
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait AWSDynamoDBService extends AWSService {

  val configuration: Configuration
  val client: AmazonDynamoDBAsyncClient = new AmazonDynamoDBAsyncClient(credentials)

  def putItem(itemId: String, itemJson: JsValue, itemType: String, team: Team): Future[Unit]
  def getItem(itemId: String, itemType: String, team: Team): Future[Option[String]]

}
