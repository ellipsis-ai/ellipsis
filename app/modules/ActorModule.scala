package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import actors.{CleanUpLambdaActor, ScheduledMessageActor}

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ScheduledMessageActor](ScheduledMessageActor.name)
    bindActor[CleanUpLambdaActor](CleanUpLambdaActor.name)
  }
}
