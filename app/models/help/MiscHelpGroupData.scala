package models.help

import json.{BehaviorGroupData, BehaviorVersionData}
import services.{AWSLambdaService, DataService}
import utils.{FuzzyMatchPattern, SimpleFuzzyMatchPattern}

case class MiscHelpGroupData(groups: Seq[BehaviorGroupData]) extends HelpGroupData {
  val isMiscellaneous: Boolean = true
  val helpActionId: String = HelpGroupData.MISCELLANEOUS_ACTION_ID
  val behaviorVersions: Seq[BehaviorVersionData] = groups.flatMap(_.behaviorVersions)
  val name: String = "Miscellaneous skills"
  override def shortName: String = "Miscellaneous"
  val description: String = ""
  def editLink(dataService: DataService, lambdaService: AWSLambdaService): Option[String] = None

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern] = {
    behaviorVersions.flatMap(_.triggers)
  }

  val fuzzyMatchName: FuzzyMatchPattern = SimpleFuzzyMatchPattern(None)
  val fuzzyMatchDescription: FuzzyMatchPattern = SimpleFuzzyMatchPattern(None)
}
