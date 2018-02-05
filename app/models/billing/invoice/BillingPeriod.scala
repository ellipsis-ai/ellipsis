package models.billing.invoice

import java.time.OffsetDateTime


case class BillingPeriod(start: OffsetDateTime, end: OffsetDateTime)
