package modules

import actors.{CleanUpLambdaActor, ConversationReminderActor, ScheduledActor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ScheduledActor](ScheduledActor.name)
    bindActor[CleanUpLambdaActor](CleanUpLambdaActor.name)
    bindActor[ConversationReminderActor](ConversationReminderActor.name)
  }
}
