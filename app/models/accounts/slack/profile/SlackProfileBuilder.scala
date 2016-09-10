package models.accounts.slack.profile

import com.mohiva.play.silhouette.impl.providers.SocialProfileBuilder

trait SlackProfileBuilder {
  self: SocialProfileBuilder =>

  type Profile = SlackProfile
}
