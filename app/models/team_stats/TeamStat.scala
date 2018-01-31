package models.team_stats

import java.time.{OffsetDateTime, ZoneId}

import models.IDs
import play.api.libs.json.{JsValue, Json}

case class TeamStat(
                 id: String,
                 name: String,
                 timeId: String,
                 start_time: OffsetDateTime,
                 end_time: OffsetDateTime,
                 value: BigDecimal,
                 about: JsValue,
                 createdAt: OffsetDateTime
               ) {
}

object TeamStat {

  def apply(
             name: String,
             teamId: String,
             start_time: OffsetDateTime,
             end_time: OffsetDateTime,
             value: BigDecimal): TeamStat = {
    TeamStat(
      IDs.next,
      name,
      teamId,
      start_time,
      end_time,
      value,
      Json.parse("{}"),
      OffsetDateTime.now
    )
  }

}
