package models.behaviors.behaviorparameter

case class DataTypeResultBody(values: Seq[ValidValue])

object DataTypeResultBody {
  def empty = DataTypeResultBody(Seq())
}
