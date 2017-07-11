package models.behaviors.datatypeconfig

import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataTypeConfigForSchema {

  val typeName: String
  lazy val fieldName = GraphQLHelpers.formatFieldName(typeName)
  lazy val listName = GraphQLHelpers.formatFieldName(typeName) ++ "List"
  lazy val deleteFieldName = "delete" ++ outputName
  lazy val createFieldName = "create" ++ outputName

  lazy val outputName: String = GraphQLHelpers.formatTypeName(typeName)
  lazy val inputName: String = outputName ++ "Input"

  def dataTypeFields(dataService: DataService): Future[Seq[DataTypeFieldForSchema]]

  def outputFields(dataService: DataService): Future[String] = {
    dataTypeFields(dataService).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.output).mkString("\n  ")
    }
  }

  def outputFieldNames(dataService: DataService): Future[String] = {
    dataTypeFields(dataService).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.outputName).mkString("\n  ")
    }
  }

  def output(dataService: DataService): Future[String] = {
    outputFields(dataService).map { fieldsStr =>
      s"""type ${outputName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }

  def inputFields(dataService: DataService): Future[String] = {
    dataTypeFields(dataService).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.input).mkString("\n  ")
    }
  }

  def input(dataService: DataService): Future[String] = {
    inputFields(dataService).map { fieldsStr =>
      s"""input ${inputName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }

  def graphQL(dataService: DataService): Future[String] = {
    for {
      input <- input(dataService)
      output <- output(dataService)
    } yield s"""$input\n\n$output"""
  }

  def queryFieldsString: String = {
    s"""  ${listName}(filter: ${inputName}!): [${outputName}]\n"""
  }

  def mutationFieldsString: String = {
    s"""  $createFieldName($fieldName: $inputName!): $outputName!
       |  $deleteFieldName(id: ID!): $outputName"""
  }
}
