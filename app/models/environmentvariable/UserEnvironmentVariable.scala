package models.environmentvariable

import java.time.OffsetDateTime

import models.accounts.user.User

case class UserEnvironmentVariable(
                                name: String,
                                value: String,
                                user: User,
                                createdAt: OffsetDateTime
                              ) extends EnvironmentVariable {

  def toRaw: RawUserEnvironmentVariable = {
    RawUserEnvironmentVariable(name, value, user.id, createdAt)
  }

}
