import json._
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorParameterType, NumberType, TextType, YesNoType}
import models.behaviors.datatypeconfig.LoadedDataTypeConfig
import models.behaviors.defaultstorageitem.{IdPassedForCreationException, NoIdPassedForUpdateException}
import play.api.libs.json._
import services.{DataService, GraphQLService, ItemNotFoundError}
import slick.dbio.DBIO
import support.DBSpec

class GraphQLServiceSpec extends DBSpec {

  lazy val graphQLService = app.injector.instanceOf(classOf[GraphQLService])

  def textTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(TextType, dataService))
  }

  def numberTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(NumberType, dataService))
  }

  def yesNoTypeData(dataService: DataService): BehaviorParameterTypeData = {
    runNow(BehaviorParameterTypeData.from(YesNoType, dataService))
  }

  def buildBehaviorVersionDataFor(group: BehaviorGroup, maybeName: Option[String], fields: Seq[(String, BehaviorParameterTypeData)]): BehaviorVersionData = {
    val data = BehaviorVersionData.newUnsavedFor(group.team.id, isDataType = true, isTest = false, maybeName)
    data.copy(
      config = data.config.copy(
        dataTypeConfig = Some(DataTypeConfigData(
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
          ("someType", BehaviorParameterTypeData(behaviorVersionData.id, None, behaviorVersionData.name.get, None, Some(BehaviorParameterType.typescriptTypeForDataTypes))),
          ("bar", numberTypeData(dataService)),
          ("maybe", yesNoTypeData(dataService))
        )
        val behaviorVersionData2 = buildBehaviorVersionDataFor(group, Some("SomeType2"), fieldsForData2)
         newGroupVersionDataFor(group, user).copy(
          behaviorVersions = Seq(behaviorVersionData, behaviorVersionData2)
        )}

  def assertResultHasNoData(result: JsValue) = {
    val maybeData = (result \ "data").asOpt[JsValue]
    (maybeData.isEmpty || maybeData.contains(JsNull)) mustBe true
  }

  def assertResultContainsErrorMessages(result: JsValue, messages: Seq[String]) = {
    val errors = (result \ "errors").as[Seq[JsValue]].map(ea => (ea \ "message").as[String])
    errors must have length(messages.length)
    messages.foreach { msg =>
      errors.find { ea =>
        s"""^$msg.*""".r.findFirstMatchIn(ea).isDefined
      } mustBe defined
    }
  }

  def loadedDataTypeConfigsFor(groupVersion: BehaviorGroupVersion): Seq[LoadedDataTypeConfig] = {
    val dataTypeConfigs = runNow(dataService.dataTypeConfigs.allFor(groupVersion))
    runNow(DBIO.sequence(dataTypeConfigs.map(ea => LoadedDataTypeConfig.fromAction(ea, dataService))))
  }

  "schemaFor" should {

    "build a schema" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get

        val schema = runNow(graphQLService.schemaFor(firstVersion, user))
        schema.query.fields must have length(2)

        val someTypeQueryField = schema.query.fields.find(_.name == someType.behaviorVersion.listName).get
        someTypeQueryField.arguments must have length(1)
        val filterArg = someTypeQueryField.arguments.head
        filterArg.name mustBe "filter"
        filterArg.argumentType.namedType.name mustBe someType.behaviorVersion.inputName

        val mutation = schema.mutation.get
        mutation.fields must have length(8)

        val someTypeCreateField = mutation.fields.find(_.name == someType.behaviorVersion.createFieldName).get
        someTypeCreateField.arguments must have length(1)
        val createArg = someTypeCreateField.arguments.head
        createArg.name mustBe someType.behaviorVersion.fieldName
        createArg.argumentType.namedType.name mustBe someType.behaviorVersion.inputName

        val someTypeUpdateField = mutation.fields.find(_.name == someType.behaviorVersion.createFieldName).get
        someTypeUpdateField.arguments must have length(1)
        val updateArg = someTypeCreateField.arguments.head
        updateArg.name mustBe someType.behaviorVersion.fieldName
        updateArg.argumentType.namedType.name mustBe someType.behaviorVersion.inputName

        val someTypeDeleteField = mutation.fields.find(_.name == someType.behaviorVersion.deleteFieldName).get
        someTypeDeleteField.arguments must have length(1)
        val idArg = someTypeDeleteField.arguments.head
        idArg.name mustBe "id"
        idArg.argumentType mustBe sangria.schema.IDType

        val someTypeDeleteWhereField = mutation.fields.find(_.name == someType.behaviorVersion.deleteWhereFieldName).get
        someTypeDeleteWhereField.arguments must have length(1)
        val deleteFilterArg = someTypeDeleteWhereField.arguments.head
        deleteFilterArg.name mustBe "filter"
        deleteFilterArg.argumentType.namedType.name mustBe someType.behaviorVersion.inputName

        println(schema.renderPretty.trim)
      })
    }

  }

  "previewSchemaFor" should {

    "build a preview schema given unsaved group data" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val dataTypeBehaviorVersions = groupData.dataTypeBehaviorVersions
        val someTypeBehaviorVersion = dataTypeBehaviorVersions.find(_.typeName == "SomeType").get

        val schema = runNow(graphQLService.previewSchemaFor(groupData))
        schema.query.fields must have length(2)

        val someTypeQueryField = schema.query.fields.find(_.name == someTypeBehaviorVersion.listName).get
        someTypeQueryField.arguments must have length(1)
        val filterArg = someTypeQueryField.arguments.head
        filterArg.name mustBe "filter"
        filterArg.argumentType.namedType.name mustBe someTypeBehaviorVersion.inputName

        val mutation = schema.mutation.get
        mutation.fields must have length(8)

        val someTypeCreateField = mutation.fields.find(_.name == someTypeBehaviorVersion.createFieldName).get
        someTypeCreateField.arguments must have length(1)
        val createArg = someTypeCreateField.arguments.head
        createArg.name mustBe someTypeBehaviorVersion.fieldName
        createArg.argumentType.namedType.name mustBe someTypeBehaviorVersion.inputName

        val someTypeUpdateField = mutation.fields.find(_.name == someTypeBehaviorVersion.updateFieldName).get
        someTypeUpdateField.arguments must have length(1)
        val updateArg = someTypeUpdateField.arguments.head
        updateArg.name mustBe someTypeBehaviorVersion.fieldName
        updateArg.argumentType.namedType.name mustBe someTypeBehaviorVersion.inputName

        val someTypeDeleteField = mutation.fields.find(_.name == someTypeBehaviorVersion.deleteFieldName).get
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
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get

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
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
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
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get
        val someType2 = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType2").get

        val mutation =
          """mutation CreateSomeType2($someType2: SomeType2Input!) {
            |  createSomeType2(someType2: $someType2) {
            |    id
            |    bar
            |    maybe
            |  }
            |}
          """.stripMargin
        val jsonData = JsObject(Map("bar" -> JsNumber(2), "someType" -> JsObject(Map("foo" -> JsString("bar"))), "maybe" -> JsTrue))
        val mutationVariables = JsObject(Map("someType2" -> jsonData)).toString
        val mutationResult = runNow(graphQLService.runQuery(firstVersion.group, user, mutation, None, Some(mutationVariables)))

        val savedSomeTypes = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, Json.obj(), firstVersion))
        savedSomeTypes must have length(1)
        val savedSomeType = savedSomeTypes.head
        (savedSomeType.data \ "foo").as[String] mustBe "bar"

        val savedSomeType2s = runNow(dataService.defaultStorageItems.filter(someType2.behaviorVersion.typeName, Json.obj(), firstVersion))
        savedSomeType2s must have length(1)
        val savedSomeType2 = savedSomeType2s.head
        (savedSomeType2.data \ "bar").as[Double] mustBe 2
        (savedSomeType2.data \ "id").as[String] mustBe savedSomeType2.id
        (savedSomeType2.data \ "maybe").as[Boolean] mustBe true

        (mutationResult \ "data").get mustBe JsObject(Map(
          "createSomeType2" -> JsObject(Map("bar" -> JsNumber(2), "id" -> JsString(savedSomeType2.id), "maybe" -> JsTrue))))

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
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get

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
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
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

        val remainingItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
        remainingItems must have length(0)
      })
    }

    "save some records and delete them" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get

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
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
        savedItems must have length(1)
        val savedItem = savedItems.head
        (savedItem.data \ "foo").as[String] mustBe "bar"
        (savedItem.data \ "id").as[String] mustBe savedItem.id
        (mutationResult \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

        val deleteMutation =
          """mutation DeleteSomeType($filter: SomeTypeInput!) {
            |  deleteWhereSomeType(filter: $filter) {
            |    foo
            |  }
            |}
          """.stripMargin
        val deleteVariables = JsObject(Map("filter" -> JsObject.empty)).toString

        val deleteResult = runNow(graphQLService.runQuery(firstVersion.group, user, deleteMutation, None, Some(deleteVariables)))
        (deleteResult \ "data").get mustBe JsObject(Map("deleteWhereSomeType" -> JsArray(Array(JsObject(Map("foo" -> JsString("bar")))))))

        val remainingItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
        remainingItems must have length(0)
      })
    }

    "save a new record, update it, query it" in {
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get

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
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
        savedItems must have length(1)
        val savedItem = savedItems.head
        (savedItem.data \ "foo").as[String] mustBe "bar"
        (savedItem.data \ "id").as[String] mustBe savedItem.id
        (mutationResult \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

        val updateMutation =
          """mutation UpdateSomeType($someType: SomeTypeInput!) {
            |  updateSomeType(someType: $someType) {
            |    foo
            |  }
            |}
          """.stripMargin
        val updateVariables = JsObject(Map("someType" -> JsObject(Map("id" -> JsString(savedItem.id), "foo" -> JsString("updated"))))).toString

        val updateResult = runNow(graphQLService.runQuery(firstVersion.group, user, updateMutation, None, Some(updateVariables)))
        (updateResult \ "data").get mustBe JsObject(Map("updateSomeType" -> JsObject(Map("foo" -> JsString("updated")))))

        val newValueQuery =
          """{
            |  someTypeList(filter: { foo: "updated" }) {
            |    foo
            |  }
            |}
          """.stripMargin
        val newValueQueryResult = runNow(graphQLService.runQuery(firstVersion.group, user, newValueQuery, None, None))
        (newValueQueryResult \ "data").get mustBe JsObject(Map("someTypeList" -> JsArray(Array(JsObject(Map("foo" -> JsString("updated")))))))

        val oldValueQuery =
          """{
            |  someTypeList(filter: { foo: "bar" }) {
            |    foo
            |  }
            |}
          """.stripMargin
        val oldValueQueryResult = runNow(graphQLService.runQuery(firstVersion.group, user, oldValueQuery, None, None))
        // should be empty
        (oldValueQueryResult \ "data").get mustBe JsObject(Map("someTypeList" -> JsArray.empty))

      })
    }

    "return an appropriate error trying to delete a nonexistent item" in {
      withEmptyDB(dataService, { () =>
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
      withEmptyDB(dataService, { () =>
        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val query: String = "some nonsense"
        val result = runNow(graphQLService.runQuery(firstVersion.group, user, query, None, None))

        assertResultHasNoData(result)
        assertResultContainsErrorMessages(result, Seq("Syntax error while parsing GraphQL query"))
      })
    }

    "return an appropriate error if the query includes nonexistent fields" in {
      withEmptyDB(dataService, { () =>
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

        assertResultHasNoData(result)
        assertResultContainsErrorMessages(result, Seq("""Query does not pass validation. Violations:\s*Cannot query field 'nonExistent' on type 'SomeType'"""))
      })
    }

    "return an appropriate error trying to create an item while passing in an ID" in {
      withEmptyDB(dataService, { () =>

        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))

        val createMutation =
          """mutation CreateSomeType($someType: SomeTypeInput!) {
            |  createSomeType(someType: $someType) {
            |    foo
            |  }
            |}
          """.stripMargin
        val id = "12345abcdef"
        val jsonData = JsObject(Map("id" -> JsString(id), "foo" -> JsString("bar")))
        val mutationVariables = JsObject(Map("someType" -> jsonData)).toString
        val result = runNow(graphQLService.runQuery(firstVersion.group, user, createMutation, None, Some(mutationVariables)))

        assertResultHasNoData(result)
        assertResultContainsErrorMessages(result, Seq(new IdPassedForCreationException(id).getMessage))
      })
    }

    "return an appropriate error trying to update an item without passing in an ID" in {
      withEmptyDB(dataService, { () =>

        val team = newSavedTeam
        val user = newSavedUserOn(team)
        val group = newSavedBehaviorGroupFor(team)
        val groupData = buildGroupDataFor(group, user)
        val firstVersion = newSavedGroupVersionFor(group, user, Some(groupData))
        val dataTypeConfigs = loadedDataTypeConfigsFor(firstVersion)
        val someType = dataTypeConfigs.find(_.behaviorVersion.typeName == "SomeType").get

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
        val savedItems = runNow(dataService.defaultStorageItems.filter(someType.behaviorVersion.typeName, jsonData, firstVersion))
        savedItems must have length(1)
        val savedItem = savedItems.head
        (savedItem.data \ "foo").as[String] mustBe "bar"
        (savedItem.data \ "id").as[String] mustBe savedItem.id
        (mutationResult \ "data").get mustBe JsObject(Map("createSomeType" -> JsObject(Map("foo" -> JsString("bar")))))

        val updateMutation =
          """mutation UpdateSomeType($someType: SomeTypeInput!) {
            |  updateSomeType(someType: $someType) {
            |    foo
            |  }
            |}
          """.stripMargin
        val updateVariables = JsObject(Map("someType" -> JsObject(Map("foo" -> JsString("updated"))))).toString

        val updateResult = runNow(graphQLService.runQuery(firstVersion.group, user, updateMutation, None, Some(updateVariables)))

        assertResultHasNoData(updateResult)
        assertResultContainsErrorMessages(updateResult, Seq(new NoIdPassedForUpdateException().getMessage))
      })
    }
  }

}
