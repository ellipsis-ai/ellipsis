package models

import support.DBSpec

class SavedAnswerServiceSpec extends DBSpec {

  "SavedAnswerService.deleteForUser" should {

    "delete only the saved answer for a given user, if any" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val anotherUser = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val param = newSavedParamFor(version, isSavedForUser = Some(true))
        val input = param.input
        val savedAnswer = newSavedAnswerFor(input, user)
        val anotherSavedAnswer = newSavedAnswerFor(input, anotherUser)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq(savedAnswer)
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq(anotherSavedAnswer)

        runNow(dataService.savedAnswers.deleteForUser(input, user))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq(anotherSavedAnswer)

      })
    }

  }

  "SavedAnswerService.deleteAllFor" should {

    "delete for all users" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val anotherUser = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val param = newSavedParamFor(version, isSavedForUser = Some(true))
        val input = param.input
        val savedAnswer = newSavedAnswerFor(input, user)
        val anotherSavedAnswer = newSavedAnswerFor(input, anotherUser)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq(savedAnswer)
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq(anotherSavedAnswer)

        runNow(dataService.savedAnswers.deleteAllFor(input))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq()

      })
    }

    "delete team saved answers" should {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behavior = newSavedBehaviorFor(group)
        val version = newSavedVersionFor(behavior)
        val param = newSavedParamFor(version, isSavedForTeam = Some(true))
        val input = param.input
        val savedAnswer = newSavedAnswerFor(input, user)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))).map(_.id) mustBe Seq(savedAnswer.id)

        runNow(dataService.savedAnswers.deleteAllFor(input))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()

      })
    }

  }

}
