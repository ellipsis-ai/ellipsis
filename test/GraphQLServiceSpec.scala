package models

import json.{BehaviorParameterTypeData, BehaviorVersionData, DataTypeConfigData, DataTypeFieldData}
import models.behaviors.behaviorparameter.{NumberType, TextType}
import play.api.libs.json.{JsArray, JsObject, JsString}
import services.{DataService, GraphQLService}
import support.DBSpec

class GraphQLServiceSpec extends DBSpec {

  lazy val graphQLService = app.injector.instanceOf(classOf[GraphQLService])

  def textTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(TextType, dataService))
  }

  def numberTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(NumberType, dataService))
  }

  "schemaFor" should {

    "build a schema" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val behaviorVersionData =
          BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName = Some("SomeType"), dataService).
            copy(dataTypeConfig = Some(DataTypeConfigData(Seq(DataTypeFieldData.newUnsavedNamed("foo", textTypeData(dataService))), Some(false))))
        val behaviorVersionData2 =
          BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName = Some("SomeType2"), dataService).
            copy(dataTypeConfig = Some(DataTypeConfigData(Seq(
              DataTypeFieldData.newUnsavedNamed("someType", BehaviorParameterTypeData(behaviorVersionData.behaviorId, None, behaviorVersionData.name.get, None)),
              DataTypeFieldData.newUnsavedNamed("bar", numberTypeData(dataService))
            ), Some(false))))
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData, behaviorVersionData2)
        )
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.name == "SomeType").get

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
        val behaviorVersionData =
          BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName = Some("SomeType"), dataService).
            copy(dataTypeConfig = Some(DataTypeConfigData(Seq(DataTypeFieldData.newUnsavedNamed("foo", textTypeData(dataService))), Some(false))))
        val behaviorVersionData2 =
          BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName = Some("SomeType2"), dataService).
            copy(dataTypeConfig = Some(DataTypeConfigData(Seq(
              DataTypeFieldData.newUnsavedNamed("someType", BehaviorParameterTypeData(behaviorVersionData.behaviorId, None, behaviorVersionData.name.get, None)),
              DataTypeFieldData.newUnsavedNamed("bar", numberTypeData(dataService))
            ), Some(false))))
        val groupData = newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData, behaviorVersionData2)
        )
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.name == "SomeType").get

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
        val savedItem = savedItems.head
        (savedItem.data \ "foo").as[String] mustBe "bar"
        (savedItem.data \ "id").as[String] mustBe savedItem.id
        (mutationResult.get \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

//        val query =
//          """query FindSomeType($filter: SomeTypeInput!) {
//            |  someTypeList(filter: $filter) {
//            |    foo
//            |  }
//            |}
//          """.stripMargin
//        val queryVariables = JsObject(Map("filter" -> jsonData)).toString
        val query =
          """{
            |  someTypeList(filter: { foo: "bar" }) {
            |    foo
            |  }
            |}
          """.stripMargin
        val queryResult = runNow(graphQLService.runQuery(firstVersion.group, query, None, None))
        (queryResult.get \ "data").get mustBe JsObject(Map("someTypeList" -> JsArray(Array(JsObject(Map("foo" -> JsString("bar")))))))
      })
    }
  }

}
