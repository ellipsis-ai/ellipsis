package services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model.TableDescription
import models.Team
import play.api.Configuration
import play.api.libs.json.JsValue

trait AWSDynamoDBService extends AWSService {

  val configuration: Configuration
  val blockingClient: AmazonDynamoDBClient = new AmazonDynamoDBClient(credentials)
  val db: DynamoDB = new DynamoDB(blockingClient)

  def createItemsTable: TableDescription
  def putItem(itemId: String, itemJson: JsValue, itemType: String, team: Team): Unit
  def getItem(itemId: String, itemType: String, team: Team): Option[String]

}
