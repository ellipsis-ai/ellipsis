package models

import json._
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorparameter.{NumberType, TextType}
import play.api.libs.json._
import services.{DataService, GraphQLService, ItemNotFoundError}
import support.DBSpec

class GraphQLServiceSpec extends DBSpec {

  lazy val graphQLService = app.injector.instanceOf(classOf[GraphQLService])

  def textTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(TextType, dataService))
  }

  def numberTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(NumberType, dataService))
  }

  def buildBehaviorVersionDataFor(group: BehaviorGroup, maybeName: Option[String], fields: Seq[(String, BehaviorParameterTypeData)]): BehaviorVersionData = {
    val data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, maybeName, dataService)
    data.copy(
      config = data.config.copy(
        dataTypeConfig = Some(DataTypeConfigData(
          maybeName,
          fields.map { case(name, paramType) =>
            DataTypeFieldData.newUnsavedNamed(name, paramType)
          },
          Some(false)
        ))
      )
    )
  }

  def buildGroupDataFor(group: BehaviorGroup, user : User): BehaviorGroupData = {
        val behaviorVersionData = buildBehaviorVersionDataFor(group, Some("SomeType"), Seq(("foo", textTypeData(dataService))))
        val fieldsForData2 = Seq(
          ("someType", BehaviorParameterTypeData(behaviorVersionData.id, None, behaviorVersionData.name.get, None)),
          ("bar", numberTypeData(dataService))
        )
        val behaviorVersionData2 = buildBehaviorVersionDataFor(group, Some("SomeType2"), fieldsForData2)
         newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData, behaviorVersionData2)
        )}

  def assertResultContainsErrorMessages(result: JsValue, messages: Seq[String]) = {
    val errors = (result \ "errors").as[Seq[JsValue]].map(ea => (ea \ "message").as[String])
    errors must have length(messages.length)
    messages.foreach { msg =>
      errors.find { ea =>
        s"""^$msg.*""".r.findFirstMatchIn(ea).isDefined
      } mustBe defined
    }
  }

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
        val dataTypeConfigs = groupData.dataTypeBehaviorVersions.flatMap(_.config.dataTypeConfig)
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
        (savedItem.data \ "foo").as[String] mustBe "bar"
        (savedItem.data \ "id").as[String] mustBe savedItem.id
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
        (savedSomeType.data \ "foo").as[String] mustBe "bar"

        val savedSomeType2s = runNow(dataService.defaultStorageItems.filter(someType2.typeName, Json.obj(), group))
        savedSomeType2s must have length(1)
        val savedSomeType2 = savedSomeType2s.head
        (savedSomeType2.data \ "bar").as[Double] mustBe 2
        (savedSomeType2.data \ "id").as[String] mustBe savedSomeType2.id

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

    "save a new record and delete it" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(firstVersion))
        val someType = dataTypeConfigs.find(_.typeName == "SomeType").get

        val createMutation =
          """mutation CreateSomeType($someType: SomeTypeInput!) {
            |  createSomeType(someType: $someType) {
            |    foo
            |  }
            |}
          """.stripMargin
        val jsonData = JsObject(Map("foo" -> JsString("bar")))
        val mutationVariables = JsObject(Map("someType" -> jsonData)).toString
        val mutationResult = runNow(graphQLService.runQuery(firstVersion.group, user, createMutation, None, Some(mutationVariables)))
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.typeName, jsonData, group))
        savedItems must have length(1)
        val savedItem = savedItems.head
        (savedItem.data \ "foo").as[String] mustBe "bar"
        (savedItem.data \ "id").as[String] mustBe savedItem.id
        (mutationResult \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

        val deleteMutation =
        """mutation DeleteSomeType($id: ID!) {
          |  deleteSomeType(id: $id) {
          |    foo
          |  }
          |}
        """.stripMargin
        val deleteVariables = JsObject(Map("id" -> JsString(savedItem.id))).toString

        val deleteResult = runNow(graphQLService.runQuery(firstVersion.group, user, deleteMutation, None, Some(deleteVariables)))
        (deleteResult \ "data").get mustBe JsObject(Map("deleteSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

        val remainingItems = runNow(dataService.defaultStorageItems.filter(someType.typeName, jsonData, group))
        remainingItems must have length(0)
      })
    }

    "return an appropriate error trying to delete a nonexistent item" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val deleteMutation =
          """mutation DeleteSomeType($id: ID!) {
            |  deleteSomeType(id: $id) {
            |    foo
            |  }
            |}
          """.stripMargin
        val nonexistentItemId = IDs.next
        val deleteVariables = JsObject(Map("id" -> JsString(nonexistentItemId))).toString

        val deleteResult = runNow(graphQLService.runQuery(firstVersion.group, user, deleteMutation, None, Some(deleteVariables)))
        (deleteResult \ "data").get mustBe JsObject(Map("deleteSomeType" -> JsNull))
        assertResultContainsErrorMessages(deleteResult, Seq(ItemNotFoundError(nonexistentItemId).getMessage))
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
        assertResultContainsErrorMessages(result, Seq("Syntax error while parsing GraphQL query"))
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
        assertResultContainsErrorMessages(result, Seq("""Query does not pass validation. Violations:\s*Cannot query field 'nonExistent' on type 'SomeType'"""))
      })
    }
  }

}
