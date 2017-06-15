package models.behaviors.datatypeconfig

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DataTypeConfig(
                          id: String,
                          behaviorVersion: BehaviorVersion
                        ) {

  lazy val name = behaviorVersion.maybeName.getOrElse("Unnamed Type")
  lazy val pluralName = GraphQLHelpers.formatFieldName(name) ++ "s" // TODO: for realz
  lazy val graphQLListName = name.take(1).toLowerCase ++ name.substring(1) ++ "List"
  lazy val graphQLDeleteFieldName = "delete" ++ graphQLOutputName

  lazy val graphQLOutputName: String = GraphQLHelpers.formatTypeName(name)
  lazy val graphQLInputName: String = graphQLOutputName ++ "Input"

  def graphQLOutputFields(dataService: DataService): Future[String] = {
    dataService.dataTypeFields.allFor(this).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.graphQLOutput).mkString("\n  ")
    }
  }

  def graphQLOutput(dataService: DataService): Future[String] = {
    graphQLOutputFields(dataService).map { fieldsStr =>
      s"""type ${graphQLOutputName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }

  def graphQLInputFields(dataService: DataService): Future[String] = {
    dataService.dataTypeFields.allFor(this).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.graphQLInput).mkString("\n  ")
    }
  }

  def graphQLInput(dataService: DataService): Future[String] = {
    graphQLInputFields(dataService).map { fieldsStr =>
      s"""input ${graphQLInputName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }

  def graphQL(dataService: DataService): Future[String] = {
    for {
      input <- graphQLInput(dataService)
      output <- graphQLOutput(dataService)
    } yield s"""$input\n\n$output"""
  }

  def graphQLQueryFieldsString: String = {
    s"""  ${graphQLListName}(filter: ${graphQLInputName}): [${graphQLOutputName}]\n"""
  }

  def graphQLMutationFieldsString: String = {
    s"""  $pluralName($pluralName: [$graphQLInputName!]!): [$graphQLOutputName!]!
       |  $graphQLDeleteFieldName(id: ID!): $graphQLOutputName"""
  }

  def toRaw: RawDataTypeConfig = RawDataTypeConfig(id, behaviorVersion.id)
}
