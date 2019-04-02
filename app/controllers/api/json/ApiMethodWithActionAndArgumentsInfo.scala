package controllers.api.json

import models.behaviors.ActionArg

trait ApiMethodWithActionAndArgumentsInfo extends ApiMethodWithActionInfo {
  val arguments: Seq[ActionArg]
}
