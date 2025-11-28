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
    moduleCompanionFolder: String,
    autoApprovedLabel: String,
    reviewRequiredLabel: String,
    fastForwardLabel: String,
    defaultEmail: String,
    defaultUser: String
)
