package services.caching

case class DataTypeBotResultsCacheKey(
                                      parameterId: String,
                                      maybeSearchQuery: Option[String],
                                      maybeConversationId: Option[String]
                                    )
