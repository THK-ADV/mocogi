package git

case class GitConfig(
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
