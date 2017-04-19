package models.help

import json.{BehaviorGroupData, BehaviorVersionData}
import utils.{FuzzyMatchPattern, SimpleFuzzyMatchPattern}

case class MiscHelpGroupData(groups: Seq[BehaviorGroupData]) extends HelpGroupData {
  val isMiscellaneous: Boolean = true
  val helpActionId: String = HelpGroupData.MISCELLANEOUS_ACTION_ID
  val behaviorVersions: Seq[BehaviorVersionData] = groups.flatMap(_.behaviorVersions)
  val name: String = "Miscellaneous"
  val longName: String = "Miscellaneous skills"
  val description: String = ""

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern] = {
    behaviorVersions.flatMap(_.triggers)
  }

  val fuzzyMatchName: FuzzyMatchPattern = SimpleFuzzyMatchPattern(None)
  val fuzzyMatchDescription: FuzzyMatchPattern = SimpleFuzzyMatchPattern(None)
}
