package models.help

import json.{BehaviorGroupData, BehaviorVersionData}
import services.{AWSLambdaService, DataService}
import utils.{FuzzyMatchPattern, SimpleFuzzyMatchPattern}

case class SkillHelpGroupData(group: BehaviorGroupData) extends HelpGroupData {
  val isMiscellaneous: Boolean = false
  val helpActionId: String = group.id.getOrElse(HelpGroupData.MISCELLANEOUS_ACTION_ID)
  val behaviorVersions: Seq[BehaviorVersionData] = group.behaviorVersions

  val maybeName: Option[String] = group.name.map(_.trim).filter(_.nonEmpty)
  val name: String = maybeName.getOrElse("Untitled skill")

  val maybeDescription: Option[String] = group.description.map(_.trim).filter(_.nonEmpty)
  val description: String = maybeDescription.getOrElse("")
  def editLink(dataService: DataService, lambdaService: AWSLambdaService): Option[String] = {
    group.id.map(id => dataService.behaviors.editLinkFor(id, None, lambdaService.configuration))
  }

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern] = {
    Seq(fuzzyMatchName, fuzzyMatchDescription) ++ behaviorVersions.flatMap(_.triggers)
  }

  lazy val fuzzyMatchName: FuzzyMatchPattern = {
    SimpleFuzzyMatchPattern(maybeName)
  }

  lazy val fuzzyMatchDescription: FuzzyMatchPattern = {
    SimpleFuzzyMatchPattern(maybeDescription)
  }
}
