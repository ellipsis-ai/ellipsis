package models.behaviors.managedbehaviorgroup

import models.behaviors.events.UserData

case class ManagedBehaviorGroupInfo(isManaged: Boolean, maybeContactData: Option[UserData])
