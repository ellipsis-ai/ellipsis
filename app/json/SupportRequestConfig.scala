package json

import models.behaviors.events.EventUserData

case class SupportRequestConfig(
                                 containerId: String,
                                 csrfToken: Option[String],
                                 teamId: Option[String],
                                 user: Option[EventUserData]
                               )
