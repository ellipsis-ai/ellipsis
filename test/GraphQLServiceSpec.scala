package models

import json.BehaviorVersionData
import models.behaviors.behaviorparameter.{BehaviorParameterType, NumberType, TextType}
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

}
