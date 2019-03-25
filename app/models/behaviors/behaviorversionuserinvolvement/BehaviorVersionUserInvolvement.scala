package models.behaviors.behaviorversionuserinvolvement

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion

case class BehaviorVersionUserInvolvement(
                                            id: String,
                                            behaviorVersion: BehaviorVersion,
                                            user: User,
                                            createdAt: OffsetDateTime
                                          ) {

  def toRaw: RawBehaviorVersionUserInvolvement = {
    RawBehaviorVersionUserInvolvement(
      id,
      behaviorVersion.id,
      user.id,
      createdAt
    )
  }
}
