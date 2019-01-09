package services

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import services.AWSLambdaConstants._

case class AWSLambdaBehaviorCodeBuilder(
                                         behaviorVersion: BehaviorVersion,
                                         params: Seq[BehaviorParameter],
                                         isForExport: Boolean
                                      ) {

  private def decorateParams(params: Seq[BehaviorParameter]): String = {
    params.map { ea =>
      ea.input.paramType.decorationCodeFor(ea)
    }.mkString("")
  }

  def functionWithParams: String = {
    val paramNames = params.map(_.input.name)
    val paramDecoration = if (isForExport) { "" } else { decorateParams(params) }
    s"""function(${(paramNames ++ Array(CONTEXT_PARAM)).mkString(", ")}) {
       |  $paramDecoration${behaviorVersion.functionBody.trim}
       |}\n""".stripMargin
  }

  def build: String = {
    BehaviorVersion.codeFor(functionWithParams)
  }
}
