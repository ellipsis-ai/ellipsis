package models.accounts.ms_teams.profile

import com.mohiva.play.silhouette.impl.providers.SocialProfileBuilder

trait MSTeamsProfileBuilder {
  self: SocialProfileBuilder =>

  type Profile = MSTeamsProfile
}
