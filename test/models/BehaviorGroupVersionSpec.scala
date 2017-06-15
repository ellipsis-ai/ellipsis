package models

import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase}
import json.{BehaviorGroupData, BehaviorParameterTypeData, BehaviorVersionData}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{BehaviorParameterType, NumberType, TextType}
import support.DBSpec

class BehaviorGroupVersionSpec extends DBSpec {

  def reloadGroup(db: PostgresDatabase, group: BehaviorGroup): BehaviorGroup = {
    runNow(dataService.behaviorGroups.find(group.id)).get
  }

  "schemaFor" should {

    "build a schema" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, dataService).copy(name = Some("SomeType"))
        val behaviorVersionData2 = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, dataService).copy(name = Some("SomeType2"))
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData, behaviorVersionData2)
        )
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.name == "SomeType").get
        val someType2 = dataTypeConfigs.find(_.name == "SomeType2").get
        newSavedDataTypeFieldFor("foo", someType, TextType)
        val fieldType = runNow(dataService.run(BehaviorParameterType.findAction(someType.behaviorVersion.id, firstVersion, dataService))).get
        newSavedDataTypeFieldFor("someType", someType2, fieldType)
        newSavedDataTypeFieldFor("bar", someType2, NumberType)

        val schema = runNow(dataService.behaviorGroupVersions.schemaFor(firstVersion))
        schema.query.fields must have length(2)

        val someTypeQueryField = schema.query.fields.find(_.name == someType.graphQLListName).get
        someTypeQueryField.arguments must have length(1)
        val filterArg = someTypeQueryField.arguments.head
        filterArg.name mustBe "filter"
        filterArg.argumentType.namedType.name mustBe someType.graphQLInputName

        val mutation = schema.mutation.get
        mutation.fields must have length(4)

        val someTypeUpdateField = mutation.fields.find(_.name == someType.pluralName).get
        someTypeUpdateField.arguments must have length(1)
        val updateArg = someTypeUpdateField.arguments.head
        updateArg.name mustBe someType.pluralName
        updateArg.argumentType.namedType.name mustBe someType.graphQLInputName

        val someTypeDeleteField = mutation.fields.find(_.name == someType.graphQLDeleteFieldName).get
        someTypeDeleteField.arguments must have length(1)
        val idArg = someTypeDeleteField.arguments.head
        idArg.name mustBe "id"
        idArg.argumentType mustBe sangria.schema.IDType

        println(schema.renderPretty.trim)
      })
    }

  }

  "createFor" should {

    "set the current version on the group" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val firstVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        reloadGroup(db, group).maybeCurrentVersionId mustBe Some(firstVersion.id)
        val secondVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user))
        reloadGroup(db, group).maybeCurrentVersionId mustBe Some(secondVersion.id)
      })
    }

    "maintain saved answers" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val inputData = newInputDataFor()
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        val firstGroupVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val maybeInput = runNow(dataService.inputs.allForGroupVersion(firstGroupVersion)).headOption
        maybeInput.isDefined mustBe true

        val savedAnswer = newSavedAnswerFor(maybeInput.get, user)
        runNow(dataService.savedAnswers.find(maybeInput.get, user)).map(_.valueString) mustBe Some(savedAnswer.valueString)

        val groupVersionData = runNow(BehaviorGroupData.buildFor(firstGroupVersion, user, dataService)).copyForNewVersionOf(group)
        val secondGroupVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user, groupVersionData))
        val maybeSecondInputVersion = runNow(dataService.inputs.allForGroupVersion(secondGroupVersion)).headOption

        maybeSecondInputVersion.isDefined mustBe true
        val maybeSecondSavedAnswerVersion = runNow(dataService.savedAnswers.find(maybeSecondInputVersion.get, user))
        maybeSecondSavedAnswerVersion.map(_.valueString) mustBe Some(savedAnswer.valueString)
      })
    }

    "keep data type behavior version id in sync with latest group version" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)

        val dataTypeVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = true, dataService).copy(name = Some("A data type"))

        val dataTypeParamData = BehaviorParameterTypeData(
          dataTypeVersionData.id,
          dataTypeVersionData.exportId,
          dataTypeVersionData.name.get,
          None
        )

        val inputData = newInputDataFor(Some(dataTypeParamData))
        val behaviorVersionData = BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService).copy(
          inputIds = Seq(inputData.inputId.get)
        )
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(dataTypeVersionData, behaviorVersionData),
          actionInputs = Seq(inputData)
        )
        newSavedGroupVersionFor(group, user, Some(groupData))

        val groupVersionsBefore = runNow(dataService.behaviorGroupVersions.allFor(group))
        groupVersionsBefore must have length 1
        val firstDataTypeBehaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(groupVersionsBefore.head)).filter(_.isDataType).head

        val newGroupData = runNow(BehaviorGroupData.maybeFor(group.id, user, None, dataService))

        newSavedGroupVersionFor(group, user, newGroupData)

        val groupVersionsAfter = runNow(dataService.behaviorGroupVersions.allFor(group))
        groupVersionsAfter must have length 2

        val secondGroupVersion = groupVersionsAfter.sortBy(_.createdAt).reverse.head
        val secondDataTypeBehaviorVersion = runNow(dataService.behaviorVersions.allForGroupVersion(secondGroupVersion)).filter(_.isDataType).head
        secondDataTypeBehaviorVersion.id mustNot be(firstDataTypeBehaviorVersion.id)

        val secondInput = runNow(dataService.inputs.allForGroupVersion(secondGroupVersion)).head
        secondInput.paramType.id mustBe secondDataTypeBehaviorVersion.id
      })
    }

  }

}
