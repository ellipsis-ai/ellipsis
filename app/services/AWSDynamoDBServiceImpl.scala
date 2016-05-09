package services

import java.util
import javax.inject.Inject
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec
import com.amazonaws.services.dynamodbv2.document.{KeyAttribute, Table, Item}
import com.amazonaws.services.dynamodbv2.model._
import models.Team
import play.api.Configuration

class AWSDynamoDBServiceImpl @Inject() (val configuration: Configuration) extends AWSDynamoDBService {
  import AWSDynamoDBServiceImpl._

  def createItemsTable: TableDescription = {

    val attributeDefinitions = new util.ArrayList[AttributeDefinition]()
    attributeDefinitions.add(new AttributeDefinition().withAttributeName(ITEM_PRIMARY_KEY).withAttributeType("S"))

    val keySchema = new util.ArrayList[KeySchemaElement]()
    keySchema.add(new KeySchemaElement().withAttributeName(ITEM_PRIMARY_KEY).withKeyType(KeyType.HASH))

    val request = new CreateTableRequest().withTableName(ITEMS_TABLE_NAME)
      .withKeySchema(keySchema)
      .withAttributeDefinitions(attributeDefinitions)
      .withProvisionedThroughput(
        new ProvisionedThroughput()
          .withReadCapacityUnits(5L)
          .withWriteCapacityUnits(6L)
      )

    val table = db.createTable(request)

    table.waitForActive()
  }

  def itemsTable: Table = db.getTable(ITEMS_TABLE_NAME)

  def primaryKeyFor(itemId: String, itemType: String, team: Team): String = {
    s"${team.id}_${itemType}_${itemId}"
  }

  def putItem(itemId: String, itemJson: String, itemType: String, team: Team): Unit = {
    val item =
      new Item().
        withString(ITEM_PRIMARY_KEY, primaryKeyFor(itemId, itemType, team: Team)).
        withJSON(ITEM, itemJson).
        withString(ITEM_TYPE, itemType).
        withString(TEAM_ID, team.id).
        withString(ITEM_ID, itemId)

    itemsTable.putItem(item)
  }

  def getItem(itemId: String, itemType: String, team: Team): String = {
    val itemIdComponent = new KeyAttribute(ITEM_ID, itemId)
    val itemTypeComponent = new KeyAttribute(ITEM_TYPE, itemType)
    val teamIdComponent = new KeyAttribute(TEAM_ID, team.id)

    val spec = new GetItemSpec().withPrimaryKey(itemIdComponent, itemTypeComponent, teamIdComponent)
    itemsTable.getItem(spec).getJSON(ITEM)
  }
}

object AWSDynamoDBServiceImpl {
  val ITEMS_TABLE_NAME = "default_storage_items"
  val ITEM_PRIMARY_KEY = "item_primary_key"
  val ITEM_ID = "item_id"
  val TEAM_ID = "team_id"
  val ITEM_TYPE = "item_type"
  val ITEM = "item"
}
