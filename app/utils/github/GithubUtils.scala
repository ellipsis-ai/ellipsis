package utils.github

import scala.util.matching.Regex

object GithubUtils {

  val urlRegex: Regex = """https://github.com/([^/]+)/(.+)""".r

  def maybeOwnerAndNameFor(url: String): Option[(String, String)] = url match {
    case urlRegex(owner, name) => Some((owner, name))
    case _ => None
  }

  def maybeOwnerFor(url: String): Option[String] = maybeOwnerAndNameFor(url).map(_._1)

  def maybeNameFor(url: String): Option[String] = maybeOwnerAndNameFor(url).map(_._2)

}
