package utils

import scala.collection.AbstractSet

case class NonEmptyStringSet(
                              override val head: String,
                              rest: Seq[String]
                            ) extends AbstractSet[String] {
  private val set: Set[String] = Set(head) ++ rest
  val iterator: Iterator[String] = set.iterator
  def +(elem: String): Set[String] = set.+(elem)
  def -(elem: String): Set[String] = set.-(elem)
  def contains(elem: String): Boolean = set.contains(elem)
}
