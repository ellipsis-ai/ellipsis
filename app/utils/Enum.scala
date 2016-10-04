package utils

trait Enum[A] {
  trait Value { self: A => }
  val values: List[A]
  def find(name: String) = values.find(_.toString == name)
}
