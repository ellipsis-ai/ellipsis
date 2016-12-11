package models.storage.simplelistitem

import models.accounts.user.User
import models.storage.simplelist.SimpleList
import org.joda.time.LocalDateTime
import play.api.libs.json.JsValue

case class SimpleListItem(
                     id: String,
                     list: SimpleList,
                     data: JsValue,
                     user: User,
                     createdAt: LocalDateTime
                   )
