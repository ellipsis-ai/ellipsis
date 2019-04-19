package models.behaviors.ellipsisobject

import models.behaviors.ParameterWithValue
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
                           meta: Option[MetaInfo],
                           args: Seq[ArgInfo]
                             )

object EllipsisObject {

  def buildFor(
                userInfo: DeprecatedUserInfo,
                teamInfo: TeamInfo,
                eventInfo: EventInfo,
                metaInfo: Option[MetaInfo],
                parameterValues: Seq[ParameterWithValue],
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
      metaInfo,
      ArgInfo.allFor(parameterValues, metaInfo.map(_.current.inputs).getOrElse(Seq()))
    )
  }

}
