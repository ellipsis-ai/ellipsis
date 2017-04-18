package models.help

import json.{BehaviorGroupData, BehaviorVersionData}
import utils.{FuzzyMatchPattern, SimpleFuzzyMatchPattern}

case class SkillHelpGroupData(group: BehaviorGroupData) extends HelpGroupData {
  val isMiscellaneous: Boolean = false
  val maybeGroupId: Option[String] = group.id
  val behaviorVersions: Seq[BehaviorVersionData] = group.behaviorVersions

  val maybeName: Option[String] = group.name.map(_.trim).filter(_.nonEmpty)
  val name: String = maybeName.getOrElse("Untitled skill")
  val longName: String = name

  val maybeDescription: Option[String] = group.description.map(_.trim).filter(_.nonEmpty)
  val description: String = maybeDescription.getOrElse("")

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
