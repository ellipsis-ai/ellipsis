package json

case class SkillManifestItemData(
                                  name: String,
                                  id: Option[String],
                                  editor: String,
                                  description: String,
                                  active: Boolean,
                                  developmentStatus: String,
                                  managed: Boolean,
                                  lastUsed: String
                                )
