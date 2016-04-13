import models._
import play.api._
import services.SlackService

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Models.onStart()
    SlackService.start
  }

  override def onStop(app: Application): Unit = {
    Models.onStop()
    SlackService.stop
  }
}
