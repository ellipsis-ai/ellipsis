package models.behaviors.linked_github_repo

import java.time.OffsetDateTime

case class LinkedGithubRepo(
                           owner: String,
                           repo: String,
                           behaviorGroupId: String,
                           createdAt: OffsetDateTime
                           )
