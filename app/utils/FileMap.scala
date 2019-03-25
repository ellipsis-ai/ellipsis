package utils

import com.google.inject.Singleton
import javax.inject.Inject
import models.IDs
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileMap @Inject()(
                          val cacheService: CacheService,
                          implicit val ec: ExecutionContext
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

  def maybeUrlFor(fileId: String): Future[Option[String]] = {
    cacheService.get[String](keyFor(fileId)).recover {
      case e: IllegalArgumentException => None
    }
  }

  def maybeThumbnailUrlFor(fileId: String): Future[Option[String]] = {
    cacheService.get[String](thumbnailKeyFor(fileId)).recover {
      case e: IllegalArgumentException => None
    }
  }

  def maybeUrlToUseFor(fileId: String): Future[Option[String]] = {
    maybeThumbnailUrlFor(fileId).flatMap { maybeUrl =>
      maybeUrl.map(url => Future.successful(Some(url))).getOrElse {
        maybeUrlFor(fileId)
      }
    }
  }

}
