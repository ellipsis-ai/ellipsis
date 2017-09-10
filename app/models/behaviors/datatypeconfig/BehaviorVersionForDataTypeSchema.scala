package models.behaviors.datatypeconfig

import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait BehaviorVersionForDataTypeSchema {

  val typeName: String
  lazy val fieldName = GraphQLHelpers.formatFieldName(typeName)
  lazy val listName = GraphQLHelpers.formatFieldName(typeName) ++ "List"
  lazy val deleteFieldName = "delete" ++ outputName
  lazy val createFieldName = "create" ++ outputName

  lazy val outputName: String = GraphQLHelpers.formatTypeName(typeName)
  lazy val inputName: String = outputName ++ "Input"

  def dataTypeFieldsAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[DataTypeFieldForSchema]]

  def dataTypeFields(dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[DataTypeFieldForSchema]]

  def outputFields(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    dataTypeFields(dataService).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.output).mkString("\n  ")
    }
  }

  def outputFieldNamesAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[String] = {
    dataTypeFieldsAction(dataService).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.outputName).mkString("\n  ")
    }
  }

  def outputFieldNames(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    dataService.run(outputFieldNamesAction(dataService))
  }

  def output(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    outputFields(dataService).map { fieldsStr =>
      s"""type ${outputName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }

  def inputFields(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    dataTypeFields(dataService).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.input).mkString("\n  ")
    }
  }

  def input(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    inputFields(dataService).map { fieldsStr =>
      s"""input ${inputName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }

  def graphQL(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
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
