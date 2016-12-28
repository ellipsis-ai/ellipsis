package models.behaviors.builtins

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.triggers.messagetrigger.MessageTrigger
import services.slack.{MessageEvent, SlackMessageEvent}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DisplayHelpBehavior(
                                helpString: String,
                                event: MessageEvent,
                                lambdaService: AWSLambdaService,
                                dataService: DataService
                              ) extends BuiltinBehavior {

  private def triggerStringFor(messageTrigger: MessageTrigger): String = {
    if (messageTrigger.requiresBotMention)
      s"`...${messageTrigger.pattern}`"
    else
      s"`${messageTrigger.pattern}`"
  }

  private def helpStringFor(behaviorVersion: BehaviorVersion): Future[Option[String]] = {
    for {
      triggers <- dataService.messageTriggers.allFor(behaviorVersion)
      authorNames <- event match {
        case e: SlackMessageEvent => dataService.behaviors.authorNamesFor(behaviorVersion.behavior, e)
        case _ => Future.successful(Seq())
      }
    } yield {
      val nonRegexTriggers = triggers.filter({ ea => !ea.shouldTreatAsRegex }).sortBy((trigger) => MessageTrigger.sortKeyFor(trigger.pattern, trigger.shouldTreatAsRegex))
      val namedTriggers =
        if (nonRegexTriggers.isEmpty)
          triggerStringFor(triggers.head)
        else
          nonRegexTriggers.map(triggerStringFor).mkString(" ")

      val regexTriggerCount =
        if (nonRegexTriggers.isEmpty)
          triggers.tail.count({ ea => ea.shouldTreatAsRegex })
        else
          triggers.count({ ea => ea.shouldTreatAsRegex })

      val regexTriggerString =
        if (regexTriggerCount == 1)
          s" _(also matches another pattern)_"
        else if (regexTriggerCount > 1)
          s" _(also matches $regexTriggerCount other patterns)_"
        else
          s""

      val triggersString = namedTriggers + regexTriggerString
      if (triggersString.isEmpty) {
        None
      } else {
        val link = behaviorVersion.editLinkFor(lambdaService.configuration)
        val authorsString = "" //if (authorNames.isEmpty) { "" } else { "by " ++ authorNames.map(n => s"<@$n>").mkString(", ") }
        Some(s"\n$triggersString [✎]($link) $authorsString  ")
      }
    }
  }

  private def helpStringFor(behaviorVersions: Seq[BehaviorVersion], prompt: String, matchString: String): Future[String] = {
    Future.sequence(behaviorVersions.map { ea =>
      helpStringFor(ea)
    }).map(_.flatten).map { behaviorStrings =>
      if (behaviorStrings.isEmpty) {
        ""
      } else {
        s"$prompt$matchString  \n${behaviorStrings.sortBy(_.toLowerCase).mkString("")}"
      }
    }
  }

  def result: Future[BotResult] = {
    val maybeHelpSearch = Option(helpString).filter(_.trim.nonEmpty)
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
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
      skillsString <- helpStringFor(skills, "Here’s what I can do:", matchString)
      knowledgeString <- helpStringFor(knowledge, "Here’s what I know:", matchString)
    } yield {
      val endingString = if (behaviorVersions.isEmpty) {
        s"""I’m just getting started here and can’t wait to learn.
           |
           |You can ${event.installLinkFor(lambdaService)} or ${event.teachMeLinkFor(lambdaService)} yourself.""".stripMargin
      } else {
        s"You can also ${event.installLinkFor(lambdaService)} or ${event.teachMeLinkFor(lambdaService)} yourself."
      }
      val text = s"""
          |$skillsString
          |
          |$knowledgeString
          |
          |$endingString
          |""".stripMargin
      SimpleTextResult(text, forcePrivateResponse = false)
    }
  }

}
