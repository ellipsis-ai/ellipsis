package utils

import com.google.inject.Singleton
import javax.inject.Inject
import models.IDs
import services.caching.CacheService

@Singleton
class FileMap @Inject()(
                          val cacheService: CacheService
                        ) {

  private def keyFor(fileId: String): String = s"file-map-$fileId"
  private def thumbnailKeyFor(fileId: String): String = s"file-map-thumbnail-$fileId"

  def save(file: FileReference): String = {
    val fileId = IDs.next
    cacheService.set(keyFor(fileId), file.url)
    file.maybeThumbnailUrl.foreach { thumbnailUrl =>
      cacheService.set(thumbnailKeyFor(fileId), thumbnailUrl)
    }
    fileId
  }

  def maybeUrlFor(fileId: String): Option[String] = {
    try {
      cacheService.get[String](keyFor(fileId))
    } catch {
      case e: IllegalArgumentException => None
    }
  }

  def maybeThumbnailUrlFor(fileId: String): Option[String] = {
    try {
      cacheService.get[String](thumbnailKeyFor(fileId))
    } catch {
      case e: IllegalArgumentException => None
    }
  }

}
