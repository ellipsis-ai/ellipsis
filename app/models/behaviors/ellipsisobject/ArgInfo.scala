package models.behaviors.ellipsisobject

import models.behaviors.ParameterWithValue
import play.api.libs.json.JsValue

case class ArgInfo(
                    name: String,
                    value: JsValue,
                    input: Option[InputInfo]
                  )

object ArgInfo {

  def allFor(parameterValues: Seq[ParameterWithValue], inputs: Seq[InputInfo]): Seq[ArgInfo] = {
    parameterValues.map { ea =>
      val name = ea.parameter.name
      ArgInfo(name, ea.preparedValue, inputs.find(_.name == name))
    }
  }
}
