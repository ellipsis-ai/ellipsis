package json

import play.api.libs.json.Json

object ExportFormat {

  case class ExportBehaviorVersionData(
                                  functionBody: String,
                                  responseTemplate: String,
                                  params: Seq[EditorFormat.BehaviorParameterData],
                                  triggers: Seq[EditorFormat.BehaviorTriggerData]
                                  )

  implicit val exportBehaviorVersionReads = Json.reads[ExportBehaviorVersionData]
  implicit val exportBehaviorVersionWrites = Json.writes[ExportBehaviorVersionData]

}
