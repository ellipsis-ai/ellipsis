package json

case class BehaviorCategory(
                             name: String,
                             description: String,
                             behaviorVersions: Seq[BehaviorVersionData]
                             )
