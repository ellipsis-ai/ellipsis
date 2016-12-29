package org.scalatest.examples.integration.services

import com.ning.http.client.Response
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future


class ElasticsearchServiceSpec extends ServiceIntegrationSpec with MockitoSugar {
  describe("Elasticsearch Service") {
    describe("#createIndex") {
      it("Creates a new Index") {
        val indexSchema: String = """
                  |{
                  |  "aliases" : {
                  |    "small_storage_items" : {},
                  |    "pippo_index" : {}
                  |  },
                  |  "settings" : {
                  |    "number_of_shards" : 1
                  |  },
                  |  "mappings" : {
                  |    "item-type" : {
                  |      "properties" : {
                  |        "id" : { "type" : "string", "index" : "not_analyzed" }
                  |      }
                  |    }
                  |  }
                  |}""".stripMargin
        val indexName = "small_storage_v55"
        val a: Future[Int] = elasticsearchService.createIndex(indexName, indexSchema).map(_.getStatusCode)
        val b: Future[org.scalatest.compatible.Assertion] = a.map { statusCode => assert( statusCode === 200) }
        val c: Future[Future[Int]] = b.map {
          import wabisabi._
          val client = new wabisabi.Client("http://localhost:9200")
          client.verifyIndex(indexName).map(_.getStatusCode)
        }
        val e: Future[org.scalatest.compatible.Assertion] = c.map { statusCode => assert( statusCode === 200) }
        e
      }

    }
  }
}
