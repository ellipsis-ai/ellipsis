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
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        val r: Future[Int] = elasticsearchService.createIndex(indexName, indexSchema).map { risp =>
          println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
          println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
          risp.getStatusCode()
        }
        r.map { statusCode => assert( statusCode === 500) }
      }
    }
  }
}
