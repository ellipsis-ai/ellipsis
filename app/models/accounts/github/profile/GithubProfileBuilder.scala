package models.accounts.github.profile

import com.mohiva.play.silhouette.impl.providers.SocialProfileBuilder

trait GithubProfileBuilder {
  self: SocialProfileBuilder =>

  type Profile = GithubProfile
}
