package test.integration.services

import scala.concurrent.duration._
import scala.concurrent.Await
import com.ning.http.client.Response
import play.api.libs.json._


class ElasticsearchServiceSpec extends IntegrationSpec {
  "ElasticsearchService#createIndex" should {
    "Returns 200 and creates the new index" in {
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
  "ElasticsearchService#indexDoc" should {
    "Returns 200 and index the doc" in {
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
        val repsonseAsJson = Json.parse(r1.getResponseBody())
        val returnedDocId: String = (repsonseAsJson \ "_id").as[String]
        val returnedDocType: String = (repsonseAsJson \ "_type").as[String]

        val r2: Response = Await.result(elasticsearchService.getDoc(
          indexName,
          returnedDocType,
          returnedDocId), 10.seconds)
        assert(r2.getStatusCode() === 200)
        println(r2.getResponseBody())

        val r3: Response = Await.result(elasticsearchService.deleteIndex(indexName), 10.seconds)
        assert(r3.getStatusCode() === 200)
      }
  }
}
