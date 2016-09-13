package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.{SlackBotProfileActor, ScheduledMessageActor, SlackBotProfileChangeListenerActor}

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
//    bindActor[SlackBotProfileChangeListenerActor](SlackBotProfileChangeListenerActor.name)
    bindActor[SlackBotProfileActor](SlackBotProfileActor.name)
    bindActor[ScheduledMessageActor](ScheduledMessageActor.name)
  }
}
