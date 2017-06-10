package models.behaviors.defaultstorageitem

import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.libs.json.JsValue

case class DefaultStorageItem(
                             id: String,
                             behaviorGroup: BehaviorGroup,
                             data: JsValue
                             )
