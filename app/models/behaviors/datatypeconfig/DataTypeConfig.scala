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
  lazy val fieldName = GraphQLHelpers.formatFieldName(name)
  lazy val listName = name.take(1).toLowerCase ++ name.substring(1) ++ "List"
  lazy val deleteFieldName = "delete" ++ outputName
  lazy val createFieldName = "create" ++ outputName

  lazy val outputName: String = GraphQLHelpers.formatTypeName(name)
  lazy val inputName: String = outputName ++ "Input"

  def outputFields(dataService: DataService): Future[String] = {
    dataService.dataTypeFields.allFor(this).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.output).mkString("\n  ")
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
    dataService.dataTypeFields.allFor(this).map { fields =>
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

  def toRaw: RawDataTypeConfig = RawDataTypeConfig(id, behaviorVersion.id)
}
