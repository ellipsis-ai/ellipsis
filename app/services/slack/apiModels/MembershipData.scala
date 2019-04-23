package services.slack.apiModels

import java.time.{Instant, OffsetDateTime, ZoneId}

case class MembershipData(
                         id: String,
                         team_id: String,
                         name: String,
                         updated: Long,
                         deleted: Boolean,
                         is_bot: Boolean,
                         is_app_user: Boolean
                         ) {

  val lastUpdated: OffsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(updated), ZoneId.of("UTC"))

  def displayString: String = {
    s"User ${id}(${name}) on team ${team_id}: deleted: ${deleted}, last updated: ${lastUpdated}"
  }
}
