package utils

import java.util.concurrent.{Future => JFuture}
import scala.concurrent.{Future => SFuture, Promise}
import scala.util.Try

object JavaFutureWrapper {

  def wrap[T](jFuture: JFuture[T]): SFuture[T] = {
    val promise = Promise[T]()
    new Thread(new Runnable { def run() { promise.complete(Try{ jFuture.get }) }}).start()
    promise.future
  }

}
