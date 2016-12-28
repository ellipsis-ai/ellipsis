import services.DataService
import scala.concurrent.Await
import scala.concurrent.duration._

val dataService = app.injector.instanceOf(classOf[DataService])

Await.result(dataService.teams.find("ellipsis"), 10.seconds)
