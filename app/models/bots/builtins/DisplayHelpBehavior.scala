package models.bots.builtins

import models.bots.behaviorversion.BehaviorVersion
import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                                helpString: String,
                                messageContext: MessageContext,
                                lambdaService: AWSLambdaService,
                                dataService: DataService
                              ) extends BuiltinBehavior {

  private def helpStringFor(behaviorVersions: Seq[BehaviorVersion], prompt: String, matchString: String): Future[String] = {
    Future.sequence(behaviorVersions.map { ea =>
      dataService.messageTriggers.allFor(ea)
    }).map(_.flatten).map { triggersForBehaviorVersions =>
      val grouped = triggersForBehaviorVersions.groupBy(_.behaviorVersion)
      val behaviorStrings = grouped.map { case(behavior, triggers) =>
        val nonRegexTriggers = triggers.filter({ ea => !ea.shouldTreatAsRegex })
        val namedTriggers =
          if (nonRegexTriggers.isEmpty)
            s"`${triggers.head.pattern}`"
          else
            nonRegexTriggers.map { ea =>
              s"`${ea.pattern}`"
            }.mkString(" or ")

        val regexTriggerCount =
          if (nonRegexTriggers.isEmpty)
            triggers.tail.count({ ea => ea.shouldTreatAsRegex })
          else
            triggers.count({ ea => ea.shouldTreatAsRegex })

        val regexTriggerString =
          if (regexTriggerCount == 1)
            s" (also matches another pattern)"
          else if (regexTriggerCount > 1)
            s" (also matches $regexTriggerCount other patterns)"
          else
            s""

        val triggersString = namedTriggers + regexTriggerString
        val link = behavior.editLinkFor(lambdaService.configuration)
        s"\n- $triggersString [Details]($link)"
      }
      if (behaviorStrings.isEmpty) {
        ""
      } else {
        s"$prompt$matchString:${behaviorStrings.toSeq.sortBy(_.toLowerCase).mkString("")}"
      }
    }
  }

  def result: Future[BehaviorResult] = {
    val maybeHelpSearch = Option(helpString).filter(_.trim.nonEmpty)
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      matchingTriggers <- maybeTeam.map { team =>
        maybeHelpSearch.map { helpSearch =>
          dataService.messageTriggers.allMatching(helpSearch, team)
        }.getOrElse {
          dataService.messageTriggers.allActiveFor(team)
        }
      }.getOrElse(Future.successful(Seq()))
      behaviorVersions <- Future.successful(matchingTriggers.map(_.behaviorVersion).distinct)
      (skills, knowledge) <- Future.successful(behaviorVersions.partition(_.isSkill))
      matchString <- Future.successful(maybeHelpSearch.map { s =>
        s" that matches `$s`"
      }.getOrElse(""))
      skillsString <- helpStringFor(skills, "Here's what I can do", matchString)
      knowledgeString <- helpStringFor(knowledge, "Here's what I know", matchString)
    } yield {
      val endingString = if (behaviorVersions.isEmpty) {
        s"""I'm just getting started here and can't wait to learn.
           |
           |You can ${messageContext.installLinkFor(lambdaService)} or ${messageContext.teachMeLinkFor(lambdaService)} yourself.""".stripMargin
      } else {
        s"You can also ${messageContext.installLinkFor(lambdaService)} or ${messageContext.teachMeLinkFor(lambdaService)} yourself."
      }
      val text = s"""
          |$skillsString
          |
          |$knowledgeString
          |
          |$endingString
          |""".stripMargin
      SimpleTextResult(text)
    }
  }

}
