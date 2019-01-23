package services

import json.Formatting._
import models.behaviors.ParameterWithValue
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.ellipsisobject.EllipsisObject
import play.api.libs.json.{JsObject, JsString, Json}
import services.AWSLambdaConstants._

case class AWSLambdaInvocationJsonBuilder(
                                           behaviorVersion: BehaviorVersion,
                                           ellipsisObject: EllipsisObject,
                                           parameterValues: Seq[ParameterWithValue]
                                    ) {

  def build: JsObject = {
    val parameterValueData = parameterValues.map { ea => (ea.invocationName, ea.preparedValue) }
    val contextParamData = Seq(CONTEXT_PARAM -> Json.toJson(ellipsisObject))
    JsObject(parameterValueData ++ contextParamData ++ Seq(("behaviorVersionId", JsString(behaviorVersion.id))))
  }

}
