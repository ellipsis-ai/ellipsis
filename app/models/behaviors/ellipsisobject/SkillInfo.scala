package models.behaviors.ellipsisobject

import java.time.OffsetDateTime

import json.{BehaviorGroupData, UserData}

case class SkillInfo(
                      name: Option[String],
                      description: Option[String],
                      icon: Option[String],
                      actions: Seq[BehaviorInfo],
                      dataTypes: Seq[BehaviorInfo],
                      createdAt: Option[OffsetDateTime],
                      author: Option[UserData]
                    ) {

  def findActionOrDataTypeWithId(id: String): Option[BehaviorInfo] = {
    actions.find(_.id == id).orElse(dataTypes.find(_.id == id))
  }
}

object SkillInfo {

  def fromBehaviorGroupData(data: BehaviorGroupData): SkillInfo = {
    SkillInfo(
      data.name,
      data.description,
      data.icon,
      ActionInfo.allFrom(data.actionBehaviorVersions, data.actionInputs),
      DataTypeInfo.allFrom(data.dataTypeBehaviorVersions, data.dataTypeInputs),
      data.createdAt,
      data.author
    )
  }
}
