package json

import java.time.OffsetDateTime

case class ImmutableBehaviorGroupVersionData(
                                              id: String,
                                              groupId: String,
                                              teamId: String,
                                              authorId: Option[String],
                                              name: Option[String],
                                              description: Option[String],
                                              icon: Option[String],
                                              actionInputs: Seq[InputData],
                                              dataTypeInputs: Seq[InputData],
                                              behaviorVersions: Seq[BehaviorVersionData],
                                              libraryVersions: Seq[LibraryVersionData],
                                              gitSHA: Option[String],
                                              exportId: Option[String],
                                              createdAt: Option[OffsetDateTime]
                                            )

object ImmutableBehaviorGroupVersionData {

  def buildFor(groupVersionId: String, groupId: String, data: BehaviorGroupData): ImmutableBehaviorGroupVersionData = {
    ImmutableBehaviorGroupVersionData(
      groupVersionId,
      groupId,
      data.teamId,
      data.author.map(_.id),
      data.name,
      data.description,
      data.icon,
      data.actionInputs,
      data.dataTypeInputs,
      data.behaviorVersions,
      data.libraryVersions,
      data.gitSHA,
      data.exportId,
      data.createdAt
    )
  }

}
