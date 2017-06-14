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
  lazy val inputName = name ++ "Input"
  lazy val listName = name.take(1).toLowerCase ++ name.substring(1) ++ "List"

  lazy val graphQLName: String = GraphQLHelpers.formatTypeName(name)

  def graphQLFields(dataService: DataService): Future[String] = {
    dataService.dataTypeFields.allFor(this).map { fields =>
      "  " ++ fields.sortBy(_.name).map(_.graphQL).mkString("\n  ")
    }
  }

  def graphQL(dataService: DataService): Future[String] = {
    graphQLFields(dataService).map { fieldsStr =>
      s"""type ${graphQLName} {
         |$fieldsStr
         |}""".stripMargin
    }
  }


  def toRaw: RawDataTypeConfig = RawDataTypeConfig(id, behaviorVersion.id)
}
