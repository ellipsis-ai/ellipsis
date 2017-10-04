import java.time.OffsetDateTime

import data.BuiltinBehaviorPopulator
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.BehaviorResponse
import models.behaviors.builtins.BuiltinImplementation._
import models.behaviors.events.{SlackMessage, SlackMessageEvent}
import org.scalatestplus.play.PlaySpec
import services.DataService
import slack.api.SlackApiClient
import support.DBSpec
import utils.SlackTimestamp

class BuiltinTriggersSpec extends DBSpec {

  "builtin triggers" should {

    "find matching" in {

      withEmptyDB(dataService, { () =>

        val profile = runNow(dataService.slackBotProfiles.ensure(IDs.next, LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, IDs.next, IDs.next))
        val team = runNow(dataService.teams.find(profile.teamId)).get
        val user = newSavedUserOn(team)

        new BuiltinBehaviorPopulator(dataService, ec).run()

        def responsesFor(text: String): Seq[BehaviorResponse] = {
          val event = SlackMessageEvent(
            profile,
            "c123456",
            None,
            "u123456",
            SlackMessage.fromUnformattedText(text, profile.userId),
            SlackTimestamp.now,
            mock[SlackApiClient]
          )
          dataService.runNow(dataService.behaviorResponses.allFor(event, Some(team), None))
        }

        val responses = responsesFor("…schedule")
        responses must have length(1)
//          "schedule `go bananas` every day at 3pm" must fullyMatch regex(scheduleRegex withGroups(
//            "`", "go bananas", null, "every day at 3pm"
//          ))
//
//          "unschedule `go bananas`" must fullyMatch regex(unscheduleRegex withGroups(
//            "`", "go bananas"
//          ))

//        "match double-quoted triggers" in {
//          """schedule "go bananas" every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
//            "\"", "go bananas", null, "every day at 3pm"
//          ))
//
//          """unschedule "go bananas"""" must fullyMatch regex(unscheduleRegex withGroups(
//            "\"", "go bananas"
//          ))
//        }
//
//        "match single-quoted triggers" in {
//          "schedule 'go bananas' every day at 3pm" must fullyMatch regex(scheduleRegex withGroups(
//            "'", "go bananas", null, "every day at 3pm"
//          ))
//
//          "unschedule 'go bananas'" must fullyMatch regex(unscheduleRegex withGroups(
//            "'", "go bananas"
//          ))
//        }
//
//        "match even when there are nested quotes" in {
//          """schedule `"go bananas," he said` every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
//            "`", """"go bananas," he said""", null, "every day at 3pm"
//          ))
//
//          """unschedule `"go bananas," he said`""" must fullyMatch regex(unscheduleRegex withGroups(
//            "`", """"go bananas," he said"""
//          ))
//
//          """schedule "he said 'go bananas'" every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
//            "\"", "he said 'go bananas'", null, "every day at 3pm"
//          ))
//
//          """unschedule "he said 'go bananas'"""" must fullyMatch regex(unscheduleRegex withGroups(
//            "\"", "he said 'go bananas'"
//          ))
//        }
//
//        "match privately for everyone in this channel" in {
//          """schedule `go bananas` privately for everyone in this channel every day at 3pm""" must fullyMatch regex(scheduleRegex withGroups(
//            "`", "go bananas", " privately for everyone in this channel", "every day at 3pm"
//          ))
//        }
      })
    }
  }
}
