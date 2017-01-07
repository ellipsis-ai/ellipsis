package models.environmentvariable

import java.time.ZonedDateTime

import models.accounts.user.User

case class UserEnvironmentVariable(
                                name: String,
                                value: String,
                                user: User,
                                createdAt: ZonedDateTime
                              ) extends EnvironmentVariable {

  def toRaw: RawUserEnvironmentVariable = {
    RawUserEnvironmentVariable(name, value, user.id, createdAt)
  }

}
