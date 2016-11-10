package models.accounts.linkedsimpletoken

import models.accounts.simpletokenapi.SimpleTokenApi

case class LinkedSimpleToken(
                              accessToken: String,
                              userId: String,
                              api: SimpleTokenApi
                            ) {

  def toRaw = RawLinkedSimpleToken(
    accessToken,
    userId,
    api.id
  )

}
