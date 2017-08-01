package models.behaviors.datatypefield

trait FieldTypeForSchema {

  val outputName: String
  lazy val inputName: String = outputName

}
