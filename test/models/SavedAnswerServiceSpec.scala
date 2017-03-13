package models

import json.BehaviorVersionData
import support.DBSpec

class SavedAnswerServiceSpec extends DBSpec {

  "SavedAnswerService.deleteForUser" should {

    "delete only the saved answer for a given user, if any" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val anotherUser = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val paramData = newParamDataFor(isSavedForUser = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          params = Seq(paramData)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head
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

        val paramData = newParamDataFor(isSavedForUser = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          params = Seq(paramData)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head
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

        val paramData = newParamDataFor(isSavedForTeam = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          params = Seq(paramData)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head
        val input = param.input

        val savedAnswer = newSavedAnswerFor(input, user)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq(savedAnswer)

        runNow(dataService.savedAnswers.deleteAllFor(input))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()

      })
    }

  }

}
