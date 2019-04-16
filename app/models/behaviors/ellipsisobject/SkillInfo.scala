package models.behaviors.ellipsisobject

import java.time.OffsetDateTime

import json.{BehaviorGroupData, UserData}

case class SkillInfo(
                      name: Option[String],
                      description: Option[String],
                      icon: Option[String],
                      actions: Seq[ActionInfo],
                      createdAt: Option[OffsetDateTime],
                      author: Option[UserData]
                    )

object SkillInfo {

  def fromBehaviorGroupData(data: BehaviorGroupData): SkillInfo = {
    SkillInfo(
      data.name,
      data.description,
      data.icon,
      ActionInfo.allFrom(data.behaviorVersions, data.actionInputs),
      data.createdAt,
      data.author
    )
  }
}
