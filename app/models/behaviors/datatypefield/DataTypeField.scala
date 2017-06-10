package models.behaviors.datatypefield

import models.behaviors.behaviorparameter.BehaviorParameterType

case class DataTypeField(
                          id: String,
                          name: String,
                          fieldType: BehaviorParameterType,
                          configId: String
                         ) {


}
