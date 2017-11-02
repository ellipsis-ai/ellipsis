package utils

import javax.inject.Inject

import com.google.inject.Singleton
import models.IDs
import services.CacheService

@Singleton
class SlackFileMap @Inject() (
                                val cacheService: CacheService
                              ) {

  private def keyFor(fileId: String): String = s"slack-file-map-$fileId"

  def save(url: String): String = {
    val fileId = IDs.next
    cacheService.set(keyFor(fileId), url)
    fileId
  }

  def maybeUrlFor(fileId: String): Option[String] = {
    cacheService.get[String](keyFor(fileId))
  }

}
