package git

import java.util.UUID

case class GitConfig(
    gitToken: Option[UUID],
    accessToken: String,
    baseUrl: String,
    projectId: Int,
    mainBranch: String
)
