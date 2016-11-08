package models.environmentvariable

import models.accounts.user.User
import org.joda.time.DateTime

case class UserEnvironmentVariable(
                                name: String,
                                value: String,
                                user: User,
                                createdAt: DateTime
                              ) extends EnvironmentVariable {

  def toRaw: RawUserEnvironmentVariable = {
    RawUserEnvironmentVariable(name, value, user.id, createdAt)
  }

}
