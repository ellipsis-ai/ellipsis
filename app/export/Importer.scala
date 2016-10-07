package export

import scala.concurrent.Future

trait Importer[T] {

  def run: Future[T]

}
