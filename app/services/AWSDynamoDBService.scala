package services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model.TableDescription
import models.Team
import play.api.Configuration

trait AWSDynamoDBService extends AWSService {

  val configuration: Configuration
  val blockingClient: AmazonDynamoDBClient = new AmazonDynamoDBClient(credentials)
  val db: DynamoDB = new DynamoDB(blockingClient)

  def createItemsTable: TableDescription
  def putItem(itemId: String, itemJson: String, itemType: String, team: Team): Unit
  def getItem(itemId: String, itemType: String, team: Team): String

}
