import models.bots.DefaultBehaviors
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    DefaultBehaviors.ensureForAll
  }

}
