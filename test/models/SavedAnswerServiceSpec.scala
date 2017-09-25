package models

import json.BehaviorVersionData
import support.DBSpec

class SavedAnswerServiceSpec extends DBSpec {

  "SavedAnswerService.deleteForUser" should {

    "delete only the saved answer for a given user, if any" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val anotherUser = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor(isSavedForUser = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, maybeName = None, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head
        val input = param.input

        val savedAnswer = newSavedAnswerFor(input, user)
        val anotherSavedAnswer = newSavedAnswerFor(input, anotherUser)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq(savedAnswer)
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq(anotherSavedAnswer)

        runNow(dataService.savedAnswers.deleteForUser(input.inputId, user))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq(anotherSavedAnswer)

      })
    }

  }

  "SavedAnswerService.deleteAllFor" should {

    "delete for all users" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val anotherUser = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor(isSavedForUser = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, maybeName = None, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head
        val input = param.input

        val savedAnswer = newSavedAnswerFor(input, user)
        val anotherSavedAnswer = newSavedAnswerFor(input, anotherUser)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq(savedAnswer)
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq(anotherSavedAnswer)

        runNow(dataService.savedAnswers.deleteAllFor(input.inputId))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()
        runNow(dataService.savedAnswers.allFor(anotherUser, Seq(param))) mustBe Seq()

      })
    }

    "delete team saved answers" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor(isSavedForTeam = Some(true))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, maybeName = None, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val groupVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val behaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersion)).head
        val param = runNow(dataService.behaviorParameters.allFor(behaviorVersion)).head
        val input = param.input

        val savedAnswer = newSavedAnswerFor(input, user)

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq(savedAnswer)

        runNow(dataService.savedAnswers.deleteAllFor(input.inputId))

        runNow(dataService.savedAnswers.allFor(user, Seq(param))) mustBe Seq()

      })
    }

  }

}
