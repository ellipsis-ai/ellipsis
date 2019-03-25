package utils

trait FileReference {
  val url: String
  val maybeThumbnailUrl: Option[String]
}
