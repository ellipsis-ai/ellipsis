package models.behaviors.ellipsisobject

import json.BehaviorGroupData

case class MetaInfo(
                     current: BehaviorInfo,
                     skill: SkillInfo
                   )

object MetaInfo {

  def maybeFor(behaviorId: String, behaviorGroupData: BehaviorGroupData): Option[MetaInfo] = {
    val skillInfo = SkillInfo.fromBehaviorGroupData(behaviorGroupData)
    skillInfo.findActionOrDataTypeWithId(behaviorId).map { current =>
      MetaInfo(current, skillInfo)
    }
  }

}
