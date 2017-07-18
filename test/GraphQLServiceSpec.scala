package models

import json._
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{NumberType, TextType}
import play.api.libs.json._
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

  def buildGroupDataFor(group: BehaviorGroup, user : User): BehaviorGroupData = {
        val behaviorVersionData =
          BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName = Some("SomeType"), dataService).
            copy(dataTypeConfig = Some(DataTypeConfigData(Some("SomeType"), Seq(DataTypeFieldData.newUnsavedNamed("foo", textTypeData(dataService))), Some(false))))
        val behaviorVersionData2 =
          BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName = Some("SomeType2"), dataService).
            copy(dataTypeConfig = Some(DataTypeConfigData(Some("SomeType2"), Seq(
              DataTypeFieldData.newUnsavedNamed("someType", BehaviorParameterTypeData(behaviorVersionData.id, None, behaviorVersionData.name.get, None)),
              DataTypeFieldData.newUnsavedNamed("bar", numberTypeData(dataService))
            ), Some(false))))
         newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData, behaviorVersionData2)
        )}

  "schemaFor" should {

    "build a schema" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.typeName == "SomeType").get

        val schema = runNow(graphQLService.schemaFor(firstVersion, user))
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

  "previewSchemaFor" should {

    "build a preview schema given unsaved group data" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val dataTypeConfigs = groupData.dataTypeBehaviorVersions.flatMap(_.dataTypeConfig)
        val someType = dataTypeConfigs.find(_.typeName == "SomeType").get

        val schema = runNow(graphQLService.previewSchemaFor(groupData))
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

    "save a new record and get it back" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.typeName == "SomeType").get

        val mutation =
          """mutation CreateSomeType($someType: SomeTypeInput!) {
             |  createSomeType(someType: $someType) {
             |    foo
             |  }
             |}
           """.stripMargin
        val jsonData = JsObject(Map("foo" -> JsString("bar")))
        val mutationVariables = JsObject(Map("someType" -> jsonData)).toString
        val mutationResult = runNow(graphQLService.runQuery(firstVersion.group, user, mutation, None, Some(mutationVariables)))
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.typeName, jsonData, group))
        savedItems must have length(1)
        val savedItem = savedItems.head
        (savedItem.dataWithId \ "foo").as[String] mustBe "bar"
        (savedItem.dataWithId \ "id").as[String] mustBe savedItem.id
        (mutationResult \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

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
        val queryResult = runNow(graphQLService.runQuery(firstVersion.group, user, query, None, None))
        (queryResult \ "data").get mustBe JsObject(Map("someTypeList" -> JsArray(Array(JsObject(Map("foo" -> JsString("bar")))))))
      })
    }

    "save related data and get back a nested result" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.typeName == "SomeType").get
        val someType2 = dataTypeConfigs.find(_.typeName == "SomeType2").get

        val mutation =
          """mutation CreateSomeType2($someType2: SomeType2Input!) {
            |  createSomeType2(someType2: $someType2) {
            |    id
            |    bar
            |  }
            |}
          """.stripMargin
        val jsonData = JsObject(Map("bar" -> JsNumber(2), "someType" -> JsObject(Map("foo" -> JsString("bar")))))
        val mutationVariables = JsObject(Map("someType2" -> jsonData)).toString
        val mutationResult = runNow(graphQLService.runQuery(firstVersion.group, user, mutation, None, Some(mutationVariables)))

        val savedSomeTypes = runNow(dataService.defaultStorageItems.filter(someType.typeName, Json.obj(), group))
        savedSomeTypes must have length(1)
        val savedSomeType = savedSomeTypes.head
        (savedSomeType.dataWithId \ "foo").as[String] mustBe "bar"

        val savedSomeType2s = runNow(dataService.defaultStorageItems.filter(someType2.typeName, Json.obj(), group))
        savedSomeType2s must have length(1)
        val savedSomeType2 = savedSomeType2s.head
        (savedSomeType2.dataWithId \ "bar").as[Double] mustBe 2
        (savedSomeType2.dataWithId \ "id").as[String] mustBe savedSomeType2.id

        (mutationResult \ "data").get mustBe JsObject(Map(
          "createSomeType2" -> JsObject(Map("bar" -> JsNumber(2), "id" -> JsString(savedSomeType2.id)))))

        val query =
        """{
          |  someTypeList(filter: { foo: "bar" }) {
          |    foo
          |  }
          |}
        """.stripMargin
        val queryResult = runNow(graphQLService.runQuery(firstVersion.group, user, query, None, None))
        (queryResult \ "data").get mustBe JsObject(Map("someTypeList" -> JsArray(Array(JsObject(Map("foo" -> JsString("bar")))))))
      })
    }

    "return an appropriate error if the query doesn't parse" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val query: String = "some nonsense"
        val result = runNow(graphQLService.runQuery(firstVersion.group, user, query, None, None))

        (result \ "data").toOption mustBe None
        val errors = (result \ "errors").as[Seq[String]]
        errors must have length(1)
        """^Syntax error while parsing GraphQL query.*""".r.findFirstMatchIn(errors.head) mustBe defined
      })
    }

    "return an appropriate error if the query includes nonexistent fields" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val query =
          """{
            |  someTypeList(filter: { foo: "bar" }) {
            |    nonExistent
            |  }
            |}
          """.stripMargin
        val result = runNow(graphQLService.runQuery(firstVersion.group, user, query, None, None))

        (result \ "data").toOption mustBe None
        val errors = (result \ "errors").as[Seq[String]]
        errors must have length(1)
        println(errors.head)
        """^Query does not pass validation. Violations:\s*Cannot query field 'nonExistent' on type 'SomeType'.*""".r.findFirstMatchIn(errors.head) mustBe defined
      })
    }
  }

}
