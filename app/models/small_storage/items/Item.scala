package models.small_storage.items

import models.team.Team
import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class Item(
                 id: String,
                 kind: String,
                 team: Team,
                 createdAt: DateTime = DateTime.now,
                 updateAt: DateTime = DateTime.now,
                 data: JsValue
               ) {
  require(id != null, "id cannot be null")
  require(team != null, "team cannot be null")
  require(kind != null, "kind cannot be null")
  require(data != null, "data cannot be null")
}


