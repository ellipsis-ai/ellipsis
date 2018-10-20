package utils

import scala.collection.AbstractSet

trait NonEmptyStringSet extends AbstractSet[String] {
  def head: String
  def others: Seq[String]

  private def set: Set[String] = Set(head) ++ others
  def iterator: Iterator[String] = set.iterator
  def +(elem: String): Set[String] = set.+(elem)
  def -(elem: String): Set[String] = set.-(elem)
  def contains(elem: String): Boolean = set.contains(elem)
}
