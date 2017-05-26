package models.help

import json.BehaviorVersionData
import services.{AWSLambdaService, DataService}
import utils.{FuzzyMatchPattern, FuzzyMatchable}

trait HelpGroupData extends FuzzyMatchable {
  val isMiscellaneous: Boolean
  val helpActionId: String
  val behaviorVersions: Seq[BehaviorVersionData]
  val name: String
  def shortName: String = name
  val description: String
  def maybeEditLink(dataService: DataService, lambdaService: AWSLambdaService): Option[String]

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern]
  val fuzzyMatchName: FuzzyMatchPattern
  val fuzzyMatchDescription: FuzzyMatchPattern
}

object HelpGroupData {
  val MISCELLANEOUS_ACTION_ID = "(untitled)"
}
