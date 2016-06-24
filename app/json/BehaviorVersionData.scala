package json

trait BehaviorVersionData {

  val functionBody: String
  val responseTemplate: String
  val params: Seq[EditorFormat.BehaviorParameterData]
  val triggers: Seq[EditorFormat.BehaviorTriggerData]

}
