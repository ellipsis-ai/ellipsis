import models._
import models.bots.DefaultBehaviors
import play.api._
import services.SlackService

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Models.onStart()
    DefaultBehaviors.ensureForAll
    SlackService.start
  }

  override def onStop(app: Application): Unit = {
    Models.onStop()
    SlackService.stop
  }
}
