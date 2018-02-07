package models.behaviors.behaviorgroupversionsha

import java.time.OffsetDateTime

case class BehaviorGroupVersionSHA(groupVersionId: String, gitSHA: String, createdAt: OffsetDateTime)
