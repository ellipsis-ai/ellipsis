package test.integration.services

import scala.concurrent.duration._
import scala.concurrent.Await
import com.ning.http.client.Response
import play.api.libs.json._

class ElasticsearchServiceSpec extends IntegrationSpec {
  describe("Elasticsearch Service") {
    describe("#createIndex") {
      it("Returns 200 and creates the new index") {
        val indexName = "test_small_storage_v55"
        Await.result(elasticsearchService.deleteIndex(indexName), 10.seconds)

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
        val r: Response = Await.result(elasticsearchService.createIndex(indexName, indexSchema), 10.seconds)
        assert(r.getStatusCode() === 200)

        val r1: Response = Await.result(elasticsearchService.verifyIndex(indexName), 10.seconds)
        assert(r1.getStatusCode() === 200)

        val r2: Response = Await.result(elasticsearchService.deleteIndex(indexName), 10.seconds)
        assert(r2.getStatusCode() === 200)
      }
    }
    describe("#indexDoc") {
      it("Returns 200 and index the doc") {
        val indexName = "test_small_storage_v55"
        Await.result(elasticsearchService.deleteIndex(indexName), 10.seconds)

        val indexSchema: String =
          """
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
        val r: Response = Await.result(elasticsearchService.createIndex(indexName, indexSchema), 10.seconds)
        assert(r.getStatusCode() === 200)

        val doc: JsValue = Json.parse(
          """
            |{
            |  "name" : "Watership Down",
            |  "location" : {
            |    "lat" : 51.235685,
            |    "long" : -1.309197
            | },
            | "residents" : [ {
            |    "name" : "Fiver",
            |    "age" : 4,
            |    "role" : null
            |  }, {
            |    "name" : "Bigwig",
            |    "age" : 6,
            |    "role" : "Owsla"
            |  } ]
            |}
            |""".stripMargin)
        val r1: Response = Await.result(elasticsearchService.indexDoc(
          indexName = indexName,
          docType = "pippo-type",
          json = doc
        ), 10.seconds)
        assert(r1.getStatusCode() === 201)
        println(r1.getResponseBody())

        val r2: Response = Await.result(elasticsearchService.deleteIndex(indexName), 10.seconds)
        assert(r2.getStatusCode() === 200)
      }
    }
  }
}
