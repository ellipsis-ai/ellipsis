package models.behaviors.ellipsisobject

import models.behaviors.invocationtoken.InvocationToken
import models.environmentvariable.EnvironmentVariable

case class EllipsisObject(
                               apiBaseUrl: String,
                               token: String,
                               env: Map[String, String],
                               userInfo: DeprecatedUserInfo,
                               teamInfo: TeamInfo, // deprecated key
                               team: TeamInfo,
                               event: EventInfo,
                               action: ActionInfo
                             )

object EllipsisObject {

  def buildFor(
                userInfo: DeprecatedUserInfo,
                teamInfo: TeamInfo,
                eventInfo: EventInfo,
                actionInfo: ActionInfo,
                environmentVariables: Seq[EnvironmentVariable],
                apiBaseUrl: String,
                token: InvocationToken
              ): EllipsisObject = {
    EllipsisObject(
      apiBaseUrl,
      token.id,
      environmentVariables.map { ea =>
        ea.name -> ea.value
      }.toMap,
      userInfo,
      teamInfo,
      teamInfo,
      eventInfo,
      actionInfo
    )
  }

}
