package services

import javax.inject.Inject
import com.amazonaws.services.dynamodbv2.model._
import models.team.Team
import play.api.Configuration
import play.api.libs.json.JsValue
import utils.JavaFutureConverter
import collection.JavaConverters._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AWSDynamoDBServiceImpl @Inject() (val configuration: Configuration) extends AWSDynamoDBService {
  import AWSDynamoDBConstants._

  def primaryKeyFor(itemId: String, itemType: String, team: Team): String = {
    s"${team.id}_${itemType}_${itemId}"
  }

  def putItem(itemId: String, itemJson: JsValue, itemType: String, team: Team): Future[Unit] = {

    val itemMap: java.util.Map[String, AttributeValue] = Map(
      ITEM_PRIMARY_KEY -> new AttributeValue(primaryKeyFor(itemId, itemType, team: Team)),
      ITEM -> new AttributeValue(itemJson.toString),
      ITEM_TYPE -> new AttributeValue(itemType),
      TEAM_ID -> new AttributeValue(team.id),
      ITEM_ID -> new AttributeValue(itemId)
    ).asJava

    val request =
      new PutItemRequest().
        withTableName(ITEMS_TABLE_NAME).
        withItem(itemMap)

    JavaFutureConverter.javaToScala(client.putItemAsync(request)).map(_ => Unit)
  }

  def getItem(itemId: String, itemType: String, team: Team): Future[Option[String]] = {
    val request =
      new GetItemRequest().
        withTableName(ITEMS_TABLE_NAME).
        withKey(Map(ITEM_PRIMARY_KEY -> new AttributeValue(primaryKeyFor(itemId, itemType, team))).asJava)

    JavaFutureConverter.javaToScala(client.getItemAsync(request)).map { result =>
      val item = result.getItem
      if (item == null) {
        None
      } else {
        item.asScala.get(ITEM).map(_.getS)
      }
    }
  }
}
