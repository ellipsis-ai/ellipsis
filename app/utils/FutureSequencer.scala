package utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FutureSequencer {

  private def sequenceList[T, U](items: List[U], fn: U => Future[T]): Future[List[T]] = {
    items.headOption.map { item =>
      fn(item).flatMap { result =>
        sequenceList(items.tail, fn).map { tailResults =>
          (result :: tailResults)
        }
      }
    }.getOrElse {
      Future.successful(List())
    }
  }

  def sequence[T, U](items: Seq[U], fn: U => Future[T]): Future[Seq[T]] = {
    sequenceList(items.toList, fn).map(_.toSeq)
  }

}
