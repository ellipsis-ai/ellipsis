import models._
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Models.onStart()
  }

  override def onStop(app: Application): Unit = {
    Models.onStop()
  }
}
