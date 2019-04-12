package support

import java.time.OffsetDateTime

import json.{BehaviorConfig, BehaviorGroupData, BehaviorParameterTypeData, BehaviorVersionData, DataTypeConfigData, InputData, UserData}
import models.IDs

object BehaviorGroupDataBuilder {

  def buildFor(teamId: String): BehaviorGroupData = {
    val testUser = UserData(
      ellipsisUserId = "test",
      context = Some("test"),
      userName = Some("tester"),
      userIdForContext = None,
      fullName = None,
      email = None,
      timeZone = None,
      formattedLink = None
    )
    val actionInput = InputData(
      Some(IDs.next),
      Some(IDs.next),
      Some(IDs.next),
      "input1",
      Some(BehaviorParameterTypeData(Some("Text"), Some("Text"), "Text", None, Some("string"))),
      "A question?",
      isSavedForTeam = false,
      isSavedForUser = false
    )

    val dataTypeInput = InputData(
      Some(IDs.next),
      Some(IDs.next),
      Some(IDs.next),
      "input2",
      Some(BehaviorParameterTypeData(Some("Text"), Some("Text"), "Text", None, Some("string"))),
      "Another question?",
      isSavedForTeam = false,
      isSavedForUser = false
    )

    val behaviorGroupId = IDs.next
    val action1 = BehaviorVersionData(
      id = Some(IDs.next),
      teamId = teamId,
      behaviorId = Some(IDs.next),
      groupId = Some(behaviorGroupId),
      isNew = None,
      name = Some("Action 1"),
      description = None,
      functionBody = "module.exports = function(ellipsis) {}",
      responseTemplate = "{successResult}",
      inputIds = Seq(actionInput.inputId.get),
      triggers = Seq(),
      config = BehaviorConfig(Some(IDs.next), name = None, responseTypeId = "Normal", canBeMemoized = Some(false), isDataType = false, isTest = Some(false), dataTypeConfig = None),
      exportId = Some(IDs.next),
      createdAt = Some(OffsetDateTime.now)
    )

    val dataType1 = BehaviorVersionData(
      id = Some(IDs.next),
      teamId = teamId,
      behaviorId = Some(IDs.next),
      groupId = Some(behaviorGroupId),
      isNew = None,
      name = Some("DataType1"),
      description = None,
      functionBody = "module.exports = function(ellipsis) {}",
      responseTemplate = "",
      inputIds = Seq(dataTypeInput.inputId.get),
      triggers = Seq(),
      config = BehaviorConfig(Some(IDs.next), name = None, responseTypeId = "Normal", canBeMemoized = Some(false), isDataType = true, isTest = Some(false), dataTypeConfig = Some(DataTypeConfigData(fields = Seq(), usesCode = Some(true)))),
      exportId = Some(IDs.next),
      createdAt = Some(OffsetDateTime.now)
    )

    BehaviorGroupData(
      id = Some(behaviorGroupId),
      teamId = IDs.next,
      name = Some("My Skill"),
      description = Some("has a description"),
      icon = Some("\uD83E\uDD5C"),
      actionInputs = Seq(actionInput),
      dataTypeInputs = Seq(dataTypeInput),
      behaviorVersions = Seq(action1, dataType1),
      libraryVersions = Seq.empty,
      requiredAWSConfigs = Seq.empty,
      requiredOAuthApiConfigs = Seq.empty,
      requiredSimpleTokenApis = Seq.empty,
      gitSHA = None,
      exportId = Some(IDs.next),
      createdAt = Some(OffsetDateTime.now),
      author = Some(testUser),
      deployment = None,
      metaData = None,
      isManaged = false,
      managedContact = None,
      linkedGithubRepo = None
    )
  }

}
