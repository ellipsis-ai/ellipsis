package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.{ScheduledMessageActor, SlackBotProfileChangeListenerActor}

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[SlackBotProfileChangeListenerActor](SlackBotProfileChangeListenerActor.name)
    bindActor[ScheduledMessageActor](ScheduledMessageActor.name)
  }
}
