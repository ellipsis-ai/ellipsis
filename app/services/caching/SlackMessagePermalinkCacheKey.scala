package services.caching

case class SlackMessagePermalinkCacheKey(messageTs: String, channel: String, slackTeamId: String)
