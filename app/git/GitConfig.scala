package git

import java.util.UUID

case class GitConfig(
    gitToken: Option[UUID],
    accessToken: String,
    baseUrl: String,
    projectId: Int,
    mainBranch: Branch,
    draftBranch: Branch,
    modulesFolder: String,
    coreFolder: String,
    moduleCatalogsFolder: String,
    autoApprovedLabel: String,
    reviewRequiredLabel: String,
    defaultEmail: String,
    defaultUser: String
)
