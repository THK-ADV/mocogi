package git

import java.util.UUID

case class GitConfig(
    gitToken: Option[UUID],
    accessToken: String,
    moduleModeToken: Option[UUID],
    baseUrl: String,
    projectId: Int,
    mainBranch: String,
    modulesRootFolder: String,
    coreRootFolder: String,
    autoApprovedLabel: String,
    reviewApprovedLabel: String
)
