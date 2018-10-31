package legacyjson

import json.{BehaviorConfig, BehaviorTriggerData, BehaviorVersionData, DataTypeConfigData}
import models.IDs
import models.behaviors.behaviorversion.{Normal, Private, Threaded}
import models.behaviors.triggers.{MessageSent, ReactionAdded}
import org.scalatestplus.play.PlaySpec

class BehaviorVersionDataSpec extends PlaySpec {
  val teamId: String = IDs.next
  val functionBody = "ellipsis.success('tada')"
  val response = "a response"
  val inputId1 = IDs.next
  val inputId2 = IDs.next
  val exportId: String = IDs.next

  "fromStrings" should {
    "handle legacy trigger and behavior config JSON data" in {
      val fromStrings = BehaviorVersionData.fromStrings(
        teamId = teamId,
        maybeDescription = None,
        function =
          s"""function(bar, foo) {
             |  ${functionBody}
             |}""".stripMargin,
        response = response,
        params = s"""["${inputId1}", "${inputId2}"]""",
        triggers =
          s"""[ {
             |  "text" : "bar",
             |  "requiresMention" : false,
             |  "isRegex" : false,
             |  "caseSensitive" : false
             |}, {
             |  "text" : "foo",
             |  "requiresMention" : true,
             |  "isRegex" : false,
             |  "caseSensitive" : false
             |}]""".stripMargin,
        configString =
          s"""{
             |  "exportId" : "${exportId}",
             |  "name" : "testAction",
             |  "isDataType" : false
             |}""".stripMargin
      )
      fromStrings mustEqual BehaviorVersionData(
        id = None,
        teamId = teamId,
        behaviorId = None,
        groupId = None,
        isNew = Some(false),
        name = Some("testAction"),
        description = None,
        functionBody = functionBody,
        responseTemplate = response,
        inputIds = Seq(inputId1, inputId2),
        triggers = Seq(
          BehaviorTriggerData(
            "bar",
            requiresMention = false,
            isRegex = false,
            caseSensitive = false,
            MessageSent.toString
          ),
          BehaviorTriggerData(
            "foo",
            requiresMention = true,
            isRegex = false,
            caseSensitive = false,
            MessageSent.toString
          )
        ),
        config = BehaviorConfig(
          exportId = Some(exportId),
          name = Some("testAction"),
          responseTypeId = Normal.id,
          canBeMemoized = None,
          isDataType = false,
          isTest = None,
          dataTypeConfig = None
        ),
        exportId = Some(exportId),
        createdAt = None
      )
    }

    "convert forcePrivateResponse true to Private ResponseType" in {
      val fromStrings = BehaviorVersionData.fromStrings(
        teamId = teamId,
        maybeDescription = None,
        function =
          s"""function(bar, foo) {
             |  ${functionBody}
             |}""".stripMargin,
        response = response,
        params = s"""["${inputId1}", "${inputId2}"]""",
        triggers = "[]",
        configString =
          s"""{
             |  "exportId" : "${exportId}",
             |  "name" : "testAction",
             |  "forcePrivateResponse" : true,
             |  "isDataType" : false
             |}""".stripMargin
      )
      fromStrings mustEqual BehaviorVersionData(
        id = None,
        teamId = teamId,
        behaviorId = None,
        groupId = None,
        isNew = Some(false),
        name = Some("testAction"),
        description = None,
        functionBody = functionBody,
        responseTemplate = response,
        inputIds = Seq(inputId1, inputId2),
        triggers = Seq(),
        config = BehaviorConfig(
          exportId = Some(exportId),
          name = Some("testAction"),
          responseTypeId = Private.id,
          canBeMemoized = None,
          isDataType = false,
          isTest = None,
          dataTypeConfig = None
        ),
        exportId = Some(exportId),
        createdAt = None
      )
    }

    "add a DataTypeConfig if necessary" in {
      val fromStrings = BehaviorVersionData.fromStrings(
        teamId = teamId,
        maybeDescription = None,
        function =
          s"""function() {
             |  ${functionBody}
             |}""".stripMargin,
        response = response,
        params = "[]",
        triggers = "[]",
        configString =
          s"""{
             |  "exportId" : "${exportId}",
             |  "name" : "testDataType",
             |  "isDataType" : true
             |}""".stripMargin
      )
      fromStrings mustEqual BehaviorVersionData(
        id = None,
        teamId = teamId,
        behaviorId = None,
        groupId = None,
        isNew = Some(false),
        name = Some("testDataType"),
        description = None,
        functionBody = functionBody,
        responseTemplate = response,
        inputIds = Seq(),
        triggers = Seq(),
        config = BehaviorConfig(
          exportId = Some(exportId),
          name = Some("testDataType"),
          responseTypeId = Normal.id,
          canBeMemoized = None,
          isDataType = true,
          isTest = None,
          dataTypeConfig = Some(DataTypeConfigData.withDefaultSettings)
        ),
        exportId = Some(exportId),
        createdAt = None
      )

    }

    "respect non-legacy trigger type and response type settings" in {
      val fromStrings = BehaviorVersionData.fromStrings(
        teamId = teamId,
        maybeDescription = None,
        function =
          s"""function(bar, foo) {
             |  ${functionBody}
             |}""".stripMargin,
        response = response,
        params = s"""["${inputId1}", "${inputId2}"]""",
        triggers =
          s"""[ {
             |  "text" : "bar",
             |  "requiresMention" : false,
             |  "isRegex" : false,
             |  "caseSensitive" : false,
             |  "triggerType" : "MessageSent"
             |}, {
             |  "text" : "tada",
             |  "requiresMention" : true,
             |  "isRegex" : false,
             |  "caseSensitive" : false,
             |  "triggerType" : "ReactionAdded"
             |}]""".stripMargin,
        configString =
          s"""{
             |  "exportId" : "${exportId}",
             |  "name" : "testAction",
             |  "isDataType" : false,
             |  "responseTypeId" : "Threaded"
             |}""".stripMargin
      )
      fromStrings mustEqual BehaviorVersionData(
        id = None,
        teamId = teamId,
        behaviorId = None,
        groupId = None,
        isNew = Some(false),
        name = Some("testAction"),
        description = None,
        functionBody = functionBody,
        responseTemplate = response,
        inputIds = Seq(inputId1, inputId2),
        triggers = Seq(
          BehaviorTriggerData(
            "bar",
            requiresMention = false,
            isRegex = false,
            caseSensitive = false,
            MessageSent.toString
          ),
          BehaviorTriggerData(
            "tada",
            requiresMention = true,
            isRegex = false,
            caseSensitive = false,
            ReactionAdded.toString
          )
        ),
        config = BehaviorConfig(
          exportId = Some(exportId),
          name = Some("testAction"),
          responseTypeId = Threaded.id,
          canBeMemoized = None,
          isDataType = false,
          isTest = None,
          dataTypeConfig = None
        ),
        exportId = Some(exportId),
        createdAt = None
      )

    }
  }
}
