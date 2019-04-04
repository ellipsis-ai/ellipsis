import java.time.OffsetDateTime

import json.{BehaviorConfig, BehaviorGroupData, BehaviorParameterTypeData, BehaviorVersionData, DataTypeConfigData, InputData, UserData}
import models.IDs
import org.scalatestplus.play.PlaySpec

class BehaviorGroupDataSpec extends PlaySpec {
  val teamId = "team"
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

  val originalGroup = BehaviorGroupData(
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

  "withUnsavedNewBehavior" should {
    "copy the group with a new behavior of the desired type" in {
      val newGroupWithAction = originalGroup.withUnsavedNewBehavior(isDataType = false, isTest = false, Some("newAction"))
      newGroupWithAction.behaviorVersions.length mustBe originalGroup.behaviorVersions.length + 1
      newGroupWithAction.actionBehaviorVersions.length mustBe originalGroup.actionBehaviorVersions.length + 1
      newGroupWithAction.actionBehaviorVersions.head mustBe action1
      newGroupWithAction.dataTypeBehaviorVersions mustEqual originalGroup.dataTypeBehaviorVersions
      val newAction = newGroupWithAction.actionBehaviorVersions.last
      newAction.name mustBe Some("newAction")
      newAction.isDataType mustBe false
      newAction.isTest mustBe false
      newAction.isNew mustBe Some(true)

      val newGroupWithDataType = originalGroup.withUnsavedNewBehavior(isDataType = true, isTest = false, maybeName = Some("NewDataType"))
      newGroupWithDataType.behaviorVersions.length mustBe originalGroup.behaviorVersions.length + 1
      newGroupWithDataType.actionBehaviorVersions mustEqual originalGroup.actionBehaviorVersions
      newGroupWithDataType.dataTypeBehaviorVersions.length mustEqual originalGroup.dataTypeBehaviorVersions.length + 1
      newGroupWithDataType.dataTypeBehaviorVersions.head mustBe dataType1
      val newDataType = newGroupWithDataType.dataTypeBehaviorVersions.last
      newDataType.name mustBe Some("NewDataType")
      newDataType.isDataType mustBe true
      newDataType.isNew mustBe Some(true)

      val newGroupWithTest= originalGroup.withUnsavedNewBehavior(isDataType = false, isTest = true, maybeName = Some("newTest"))
      newGroupWithTest.behaviorVersions.length mustBe originalGroup.behaviorVersions.length + 1
      newGroupWithTest.actionBehaviorVersions.length mustEqual originalGroup.actionBehaviorVersions.length + 1
      newGroupWithTest.dataTypeBehaviorVersions mustEqual originalGroup.dataTypeBehaviorVersions
      newGroupWithTest.actionBehaviorVersions.head mustBe action1
      val newTest = newGroupWithTest.actionBehaviorVersions.last
      newTest.name mustBe Some("newTest")
      newTest.isDataType mustBe false
      newTest.isTest mustBe true
      newTest.isNew mustBe Some(true)
    }
  }

  "withUnsavedClonedBehavior" should {
    "copy the group with a cloned copy of an action ID with new inputs" in {
      val newGroupWithClonedAction = originalGroup.withUnsavedClonedBehavior(action1.behaviorId.get, None)
      newGroupWithClonedAction.behaviorVersions.length mustBe originalGroup.behaviorVersions.length + 1
      newGroupWithClonedAction.actionBehaviorVersions.length mustBe originalGroup.actionBehaviorVersions.length + 1
      newGroupWithClonedAction.actionBehaviorVersions.head mustBe action1
      newGroupWithClonedAction.dataTypeBehaviorVersions mustEqual originalGroup.dataTypeBehaviorVersions

      val clone = newGroupWithClonedAction.actionBehaviorVersions.last

      clone.id must not equal action1.id
      clone.behaviorId must not equal action1.behaviorId
      clone.exportId must not equal action1.exportId
      clone.inputIds must not equal action1.inputIds
      clone.inputIds.head must not be empty
      clone.name must not equal action1.name

      clone.inputIds.length mustEqual action1.inputIds.length
      clone.triggers mustEqual action1.triggers
      clone.functionBody mustEqual action1.functionBody
      clone.responseTemplate mustEqual action1.responseTemplate

      val groupInputIds = newGroupWithClonedAction.actionInputs.flatMap(_.inputId)
      groupInputIds must contain(action1.inputIds.head)
      groupInputIds must contain(clone.inputIds.head)
      val oldInput = newGroupWithClonedAction.actionInputs.head
      val newInput = newGroupWithClonedAction.actionInputs.last
      oldInput.inputId must not equal newInput.inputId
      oldInput.copyForComparison mustEqual newInput.copyForComparison
    }

    "copy the group with a cloned copy of a data type ID with new inputs" in {
      val newGroupWithClonedDataType = originalGroup.withUnsavedClonedBehavior(dataType1.behaviorId.get, None)
      newGroupWithClonedDataType.behaviorVersions.length mustBe originalGroup.behaviorVersions.length + 1
      newGroupWithClonedDataType.actionBehaviorVersions mustEqual originalGroup.actionBehaviorVersions
      newGroupWithClonedDataType.dataTypeBehaviorVersions.length mustBe originalGroup.dataTypeBehaviorVersions.length + 1
      newGroupWithClonedDataType.dataTypeBehaviorVersions.head mustBe dataType1

      val clone = newGroupWithClonedDataType.dataTypeBehaviorVersions.last

      clone.id must not equal dataType1.id
      clone.behaviorId must not equal dataType1.behaviorId
      clone.exportId must not equal dataType1.exportId
      clone.inputIds must not equal dataType1.inputIds
      clone.inputIds.head must not be empty
      clone.name must not equal dataType1.name

      clone.inputIds.length mustEqual dataType1.inputIds.length
      clone.triggers mustEqual dataType1.triggers
      clone.functionBody mustEqual dataType1.functionBody
      clone.responseTemplate mustEqual dataType1.responseTemplate

      val groupInputIds = newGroupWithClonedDataType.dataTypeInputs.flatMap(_.inputId)
      groupInputIds must contain(dataType1.inputIds.head)
      groupInputIds must contain(clone.inputIds.head)
      val oldInput = newGroupWithClonedDataType.dataTypeInputs.head
      val newInput = newGroupWithClonedDataType.dataTypeInputs.last
      oldInput.inputId must not equal newInput.inputId
      oldInput.copyForComparison mustEqual newInput.copyForComparison
    }
  }
}
