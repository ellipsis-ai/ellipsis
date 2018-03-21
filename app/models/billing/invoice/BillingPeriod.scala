package models.billing.invoice

import java.sql.Timestamp
import java.time.{Instant, OffsetDateTime, ZoneId}


case class BillingPeriod(start: Timestamp, end: Timestamp) {

  def startToOffsetDateTime: OffsetDateTime = {
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(start.getTime), ZoneId.systemDefault())
  }

  def endToOffsetDateTime: OffsetDateTime = {
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(end.getTime), ZoneId.systemDefault())
  }
}
