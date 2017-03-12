import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._

val client = ElasticClient.transport(ElasticsearchClientUri("localhost", 9300))
val s = client.execute { get cluster stats }.await
println(s)

//client.execute { index into "bands" / "artists" fields "name"->"coldplay" }.await
//val resp = client.execute { search in "bands" / "artists" query "coldplay" }.await
//println(resp)
