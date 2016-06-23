package json

import json.ExportFormat.ExportBehaviorVersionData
import org.joda.time.DateTime
import play.api.libs.json.Json

object EditorFormat {

  case class BehaviorParameterData(name: String, question: String)
  case class BehaviorTriggerData(
                                  text: String,
                                  requiresMention: Boolean,
                                  isRegex: Boolean,
                                  caseSensitive: Boolean
                                  )

  case class BehaviorVersionData(
                                  teamId: String,
                                  behaviorId: Option[String],
                                  functionBody: String,
                                  responseTemplate: String,
                                  params: Seq[BehaviorParameterData],
                                  triggers: Seq[BehaviorTriggerData],
                                  createdAt: Option[DateTime]
                                  ) {
    def forExport: ExportBehaviorVersionData = ExportBehaviorVersionData(
      functionBody,
      responseTemplate,
      params,
      triggers
    )
  }

  case class BehaviorData(behaviorId: String, versions: Seq[BehaviorVersionData])

  implicit val behaviorParameterReads = Json.reads[BehaviorParameterData]
  implicit val behaviorParameterWrites = Json.writes[BehaviorParameterData]

  implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

  implicit val behaviorReads = Json.reads[BehaviorData]
  implicit val behaviorWrites = Json.writes[BehaviorData]

}
