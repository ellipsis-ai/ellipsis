package models.help

import json.BehaviorVersionData
import utils.{FuzzyMatchPattern, FuzzyMatchable}

trait HelpGroupData extends FuzzyMatchable {
  val isMiscellaneous: Boolean
  val helpActionId: String
  val behaviorVersions: Seq[BehaviorVersionData]
  val name: String
  val shortName: String = name
  val description: String

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern]
  val fuzzyMatchName: FuzzyMatchPattern
  val fuzzyMatchDescription: FuzzyMatchPattern
}

object HelpGroupData {
  val MISCELLANEOUS_ACTION_ID = "(untitled)"
}
