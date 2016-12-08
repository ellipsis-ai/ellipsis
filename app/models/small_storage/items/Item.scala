package models.small_storage.items

import models.team.Team
import org.joda.time.DateTime

/**
  * Created by matteo on 12/7/16.
  */
case class Item(
                 id: String,
                 kind: String,
                 team: Team,
                 createdAt: DateTime = DateTime.now,
                 updateAt: DateTime = DateTime.now,
                 data: String
               ) {
  require(id != null, "id cannot be null")
  require(team != null, "team cannot be null")
  require(kind != null, "kind cannot be null")
  require(data != null, "data cannot be null")
}


//object Book {
//  implicit val format = Json.format[Book]
//}



