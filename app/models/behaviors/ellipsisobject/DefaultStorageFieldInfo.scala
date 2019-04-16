package models.behaviors.ellipsisobject

import json.DataTypeFieldData
import models.behaviors.behaviorparameter.TextType

case class DefaultStorageFieldInfo(
                                name: String,
                                fieldType: String,
                                isLabel: Boolean
                              )

object DefaultStorageFieldInfo {

  def fromFieldData(data: DataTypeFieldData): DefaultStorageFieldInfo = {
    DefaultStorageFieldInfo(
      data.name,
      data.fieldType.flatMap(_.id).getOrElse(TextType.id),
      data.isLabel
    )
  }
}
