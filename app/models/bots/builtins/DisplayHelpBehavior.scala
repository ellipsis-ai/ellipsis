package models.bots.builtins

import models.Team
import models.bots.{BehaviorVersion, MessageContext}
import models.bots.triggers.MessageTriggerQueries
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class DisplayHelpBehavior(helpString: String, messageContext: MessageContext, lambdaService: AWSLambdaService) extends BuiltinBehavior {

  private def helpStringFor(behaviorVersions: Seq[BehaviorVersion], prompt: String, matchString: String): DBIO[String] = {
    DBIO.sequence(behaviorVersions.map { ea =>
      MessageTriggerQueries.allFor(ea)
    }).map(_.flatten).map { triggersForBehaviorVersions =>
      val grouped = triggersForBehaviorVersions.groupBy(_.behaviorVersion)
      val behaviorStrings = grouped.map { case(behavior, triggers) =>
        val triggersString = triggers.map { ea =>
          s"`${ea.pattern}`"
        }.mkString(" or ")
        val editLink = behavior.editLinkFor(lambdaService.configuration).map { link =>
          s"[Details]($link)"
        }.getOrElse("")
        s"\n- $triggersString $editLink"
      }
      if (behaviorStrings.isEmpty) {
        ""
      } else {
        s"$prompt$matchString:${behaviorStrings.toSeq.sortBy(_.toLowerCase).mkString("")}"
      }
    }
  }

  def run: DBIO[Unit] = {
    val maybeHelpSearch = Option(helpString).filter(_.trim.nonEmpty)
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      matchingTriggers <- maybeTeam.map { team =>
        maybeHelpSearch.map { helpSearch =>
          MessageTriggerQueries.allMatching(helpSearch, team)
        }.getOrElse {
          MessageTriggerQueries.allActiveFor(team)
        }
      }.getOrElse(DBIO.successful(Seq()))
      behaviorVersions <- DBIO.successful(matchingTriggers.map(_.behaviorVersion).distinct)
      (skills, knowledge) <- DBIO.successful(behaviorVersions.partition(_.isSkill))
      matchString <- DBIO.successful(maybeHelpSearch.map { s =>
        s" that matches `$s`"
      }.getOrElse(""))
      skillsString <- helpStringFor(skills, "Here's what I can do", matchString)
      knowledgeString <- helpStringFor(knowledge, "Here's what I know", matchString)
    } yield {
      val text = s"""
          |$skillsString
          |
          |$knowledgeString
          |
          |To teach me something new, just type `@ellipsis: learn`
          |""".stripMargin
      messageContext.sendMessage(text)
    }
  }

}
