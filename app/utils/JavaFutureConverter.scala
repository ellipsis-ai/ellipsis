package utils

import java.util.concurrent.{Future => JFuture}

import scala.concurrent.duration.{Duration, TimeUnit}
import scala.concurrent.{Await, Promise, Future => SFuture}
import scala.util.Try

object JavaFutureConverter {

  def javaToScala[T](jFuture: JFuture[T]): SFuture[T] = {
    val promise = Promise[T]()
    new Thread(new Runnable { def run() { promise.complete(Try{ jFuture.get }) }}).start()
    promise.future
  }

  def scalaToJava[T](x:SFuture[T]): JFuture[T] = {
    new JFuture[T] {
      override def isCancelled: Boolean = throw new UnsupportedOperationException

      override def get(): T = Await.result(x, Duration.Inf)

      override def get(timeout: Long, unit: TimeUnit): T = Await.result(x, Duration.create(timeout, unit))

      override def cancel(mayInterruptIfRunning: Boolean): Boolean = throw new UnsupportedOperationException

      override def isDone: Boolean = x.isCompleted
    }
  }

}
