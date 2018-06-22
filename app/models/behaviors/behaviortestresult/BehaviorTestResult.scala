package models.behaviors.behaviortestresult

import java.time.OffsetDateTime

case class BehaviorTestResult(
                               id: String,
                               behaviorVersionId: String,
                               isPass: Boolean,
                               output: String,
                               runAt: OffsetDateTime
                             )
