package models.behaviors.managedbehaviorgroup

import models.behaviors.events.EventUserData

case class ManagedBehaviorGroupInfo(isManaged: Boolean, maybeContactData: Option[EventUserData])
