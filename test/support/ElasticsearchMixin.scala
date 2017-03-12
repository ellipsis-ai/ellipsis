package support

import com.typesafe.config.ConfigFactory
import services.ElasticsearchService

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ElasticsearchMixin {

  lazy val config = ConfigFactory.load()

  def withNoIndexes[T](elasticsearchService: ElasticsearchService, fn: ElasticsearchService => T) = {
    // clear all indexes
  }
}


