package support

import java.time.OffsetDateTime

import json.{BehaviorConfig, BehaviorGroupData, BehaviorParameterTypeData, BehaviorVersionData, DataTypeConfigData, InputData, UserData}
import models.IDs

object BehaviorGroupDataBuilder {

  val defaultActionInputs = Seq(
    InputData(
      Some(IDs.next),
      Some(IDs.next),
      Some(IDs.next),
      "input1",
      Some(BehaviorParameterTypeData(Some("Text"), Some("Text"), "Text", None, Some("string"))),
      "A question?",
      isSavedForTeam = false,
      isSavedForUser = false
    )
  )
  val defaultDataTypeInputs = Seq(
    InputData(
      Some(IDs.next),
      Some(IDs.next),
      Some(IDs.next),
      "input2",
      Some(BehaviorParameterTypeData(Some("Text"), Some("Text"), "Text", None, Some("string"))),
      "Another question?",
      isSavedForTeam = false,
      isSavedForUser = false
    )
  )

  def defaultActionsFor(teamId: String, behaviorGroupId: String, inputIds: Seq[String]) = Seq(
    BehaviorVersionData(
      id = Some(IDs.next),
      teamId = teamId,
      behaviorId = Some(IDs.next),
      groupId = Some(behaviorGroupId),
      isNew = None,
      name = Some("Action 1"),
      description = None,
      functionBody = "module.exports = function(ellipsis) {}",
      responseTemplate = "{successResult}",
      inputIds = inputIds,
      triggers = Seq(),
      config = BehaviorConfig(Some(IDs.next), name = None, responseTypeId = "Normal", canBeMemoized = Some(false), isDataType = false, isTest = Some(false), dataTypeConfig = None),
      exportId = Some(IDs.next),
      createdAt = Some(OffsetDateTime.now)
    )
  )

  def defaultDataTypesFor(teamId: String, behaviorGroupId: String, inputIds: Seq[String]) = Seq(
    BehaviorVersionData(
      id = Some(IDs.next),
      teamId = teamId,
      behaviorId = Some(IDs.next),
      groupId = Some(behaviorGroupId),
      isNew = None,
      name = Some("DataType1"),
      description = None,
      functionBody = "module.exports = function(ellipsis) {}",
      responseTemplate = "",
      inputIds = inputIds,
      triggers = Seq(),
      config = BehaviorConfig(Some(IDs.next), name = None, responseTypeId = "Normal", canBeMemoized = Some(false), isDataType = true, isTest = Some(false), dataTypeConfig = Some(DataTypeConfigData(fields = Seq(), usesCode = Some(true)))),
      exportId = Some(IDs.next),
      createdAt = Some(OffsetDateTime.now)
    )
  )

  val defaultAuthor = UserData(
    ellipsisUserId = "test",
    context = Some("test"),
    userName = Some("tester"),
    userIdForContext = None,
    fullName = None,
    email = None,
    timeZone = None,
    formattedLink = None
  )

  def buildFor(
                teamId: String,
                maybeGroupId: Option[String] = None,
                maybeActionInputs: Option[Seq[InputData]] = None,
                maybeDataTypeInputs: Option[Seq[InputData]] = None,
                maybeActions: Option[Seq[BehaviorVersionData]] = None,
                maybeDataTypes: Option[Seq[BehaviorVersionData]] = None,
                maybeAuthor: Option[UserData] = None
              ): BehaviorGroupData = {

    val author = maybeAuthor.getOrElse(defaultAuthor)

    val actionInputs = maybeActionInputs.getOrElse(defaultActionInputs)
    val dataTypeInputs = maybeDataTypeInputs.getOrElse(defaultDataTypeInputs)
    val behaviorGroupId = maybeGroupId.getOrElse(IDs.next)
    val actions = maybeActions.getOrElse(defaultActionsFor(teamId, behaviorGroupId, actionInputs.flatMap(_.inputId)))
    val dataTypes = maybeDataTypes.getOrElse(defaultDataTypesFor(teamId, behaviorGroupId, dataTypeInputs.flatMap(_.inputId)))

    BehaviorGroupData(
      id = Some(behaviorGroupId),
      teamId = IDs.next,
      name = Some("My Skill"),
      description = Some("has a description"),
      icon = Some("\uD83E\uDD5C"),
      actionInputs = actionInputs,
      dataTypeInputs = dataTypeInputs,
      behaviorVersions = actions ++ dataTypes,
      libraryVersions = Seq.empty,
      requiredAWSConfigs = Seq.empty,
      requiredOAuthApiConfigs = Seq.empty,
      requiredSimpleTokenApis = Seq.empty,
      gitSHA = None,
      exportId = Some(IDs.next),
      createdAt = Some(OffsetDateTime.now),
      author = Some(author),
      deployment = None,
      metaData = None,
      isManaged = false,
      managedContact = None,
      linkedGithubRepo = None
    )
  }

}
