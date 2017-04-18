package models.help

import json.BehaviorVersionData
import utils.{FuzzyMatchPattern, FuzzyMatchable}

trait HelpGroupData extends FuzzyMatchable {
  val isMiscellaneous: Boolean
  val maybeGroupId: Option[String]
  val behaviorVersions: Seq[BehaviorVersionData]
  val name: String
  val longName: String
  val description: String

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern]
  val fuzzyMatchName: FuzzyMatchPattern
  val fuzzyMatchDescription: FuzzyMatchPattern
}
