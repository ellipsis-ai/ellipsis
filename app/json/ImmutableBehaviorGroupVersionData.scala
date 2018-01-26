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
