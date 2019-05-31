package models.behaviors.form

import ai.x.play.json.Jsonx
import play.api.libs.json.Format

object Formatting {

  lazy implicit val textElementFormat: Format[TextElement] = Jsonx.formatCaseClass[TextElement]
  lazy implicit val behaviorInputsElementFormat: Format[BehaviorInputsElement] = Jsonx.formatCaseClass[BehaviorInputsElement]

  lazy implicit val formElementFormat: Format[FormElement] = Jsonx.formatSealed[FormElement]

  lazy implicit val formConfigFormat: Format[FormConfig] = Jsonx.formatCaseClass[FormConfig]

}
