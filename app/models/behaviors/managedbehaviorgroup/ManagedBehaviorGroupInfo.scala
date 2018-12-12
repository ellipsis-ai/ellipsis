package models.behaviors.managedbehaviorgroup

import json.UserData

case class ManagedBehaviorGroupInfo(isManaged: Boolean, maybeContactData: Option[UserData])
