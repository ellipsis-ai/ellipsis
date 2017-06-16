package models

import json.BehaviorVersionData
import models.behaviors.behaviorparameter.{BehaviorParameterType, NumberType, TextType}
import play.api.libs.json.{JsArray, JsObject, JsString}
import services.GraphQLService
import support.DBSpec

class GraphQLServiceSpec extends DBSpec {

  lazy val graphQLService = app.injector.instanceOf(classOf[GraphQLService])

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

        val schema = runNow(graphQLService.schemaFor(firstVersion))
        schema.query.fields must have length(2)

        val someTypeQueryField = schema.query.fields.find(_.name == someType.listName).get
        someTypeQueryField.arguments must have length(1)
        val filterArg = someTypeQueryField.arguments.head
        filterArg.name mustBe "filter"
        filterArg.argumentType.namedType.name mustBe someType.inputName

        val mutation = schema.mutation.get
        mutation.fields must have length(4)

        val someTypeCreateField = mutation.fields.find(_.name == someType.createFieldName).get
        someTypeCreateField.arguments must have length(1)
        val updateArg = someTypeCreateField.arguments.head
        updateArg.name mustBe someType.fieldName
        updateArg.argumentType.namedType.name mustBe someType.inputName

        val someTypeDeleteField = mutation.fields.find(_.name == someType.deleteFieldName).get
        someTypeDeleteField.arguments must have length(1)
        val idArg = someTypeDeleteField.arguments.head
        idArg.name mustBe "id"
        idArg.argumentType mustBe sangria.schema.IDType

        println(schema.renderPretty.trim)
      })
    }

  }

  "runQuery" should {

    "return a result" in {
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

        val mutation =
          """mutation CreateSomeType($someType: SomeTypeInput!) {
             |  createSomeType(someType: $someType) {
             |    foo
             |  }
             |}
           """.stripMargin
        val jsonData = JsObject(Map("foo" -> JsString("bar")))
        val mutationVariables = JsObject(Map("someType" -> jsonData)).toString
        val mutationResult = runNow(graphQLService.runQuery(firstVersion.group, mutation, None, Some(mutationVariables)))
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.name, jsonData, group))
        savedItems must have length(1)
        (savedItems.head.data \ "foo").as[String] mustBe "bar"
        (mutationResult.get \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

        val query =
          """query FindSomeType($filter: SomeTypeInput!) {
            |  someTypeList(filter: $filter) {
            |    foo
            |  }
            |}
          """.stripMargin
        val queryVariables = JsObject(Map("filter" -> jsonData)).toString
        val queryResult = runNow(graphQLService.runQuery(firstVersion.group, query, None, Some(queryVariables)))
        (queryResult.get \ "data").get mustBe JsObject(Map("someTypeList" -> JsArray(Array(JsObject(Map("foo" -> JsString("bar")))))))
      })
    }
  }

}
