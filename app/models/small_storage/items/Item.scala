package models.small_storage.items

import models.team.Team
import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class Item(
                 id: String,
                 kind: String,
                 createdAt: DateTime = DateTime.now,
                 updatedAt: DateTime = DateTime.now,
                 team: Team,
                 data: JsValue
               ) {
  require(id != null, "id cannot be null")
  require(team != null, "team cannot be null")
  require(createdAt != null, "createdAt cannot be null")
  require(updatedAt != null, "updatedAt cannot be null")
  require(data != null, "data cannot be null")
}


