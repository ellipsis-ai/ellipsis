package modules

import actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ScheduledActor](ScheduledActor.name)
    bindActor[CleanUpLambdaActor](CleanUpLambdaActor.name)
    bindActor[ConversationReminderActor](ConversationReminderActor.name)
    bindActor[ExpireConversationsActor](ExpireConversationsActor.name)
    bindActor[CreateFreeChargebeeSubscriptionsActor](CreateFreeChargebeeSubscriptionsActor.name)
    bindActor[ClosePendingInvoicesActor](ClosePendingInvoicesActor.name)
    bindActor[SlackMembershipActor](SlackMembershipActor.name)
  }
}
