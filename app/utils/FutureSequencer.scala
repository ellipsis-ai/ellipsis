package utils

import scala.concurrent.{ExecutionContext, Future}

object FutureSequencer {

  private def sequenceList[T, U](items: List[U], fn: U => Future[T])(implicit ec: ExecutionContext): Future[List[T]] = {
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

  def sequence[T, U](items: Seq[U], fn: U => Future[T])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    sequenceList(items.toList, fn).map(_.toSeq)
  }

}
