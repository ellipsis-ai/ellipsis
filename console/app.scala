import play.api._
val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Dev)
val context = ApplicationLoader.createContext(env)
val loader = ApplicationLoader(context)
val app = loader.load(context)
Play.start(app)

//import services.{AWSDynamoDBService, DataService}
//
//val dataService = app.injector.instanceOf(classOf[DataService])
//val dynService = app.injector.instanceOf(classOf[AWSDynamoDBService])
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//
//Await.result(dataService.teams.find("foo"), 10.seconds)

