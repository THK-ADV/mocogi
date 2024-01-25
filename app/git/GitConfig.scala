package git

import models.Branch

import java.util.UUID

case class GitConfig(
    gitToken: Option[UUID],
    accessToken: String,
    moduleModeToken: Option[UUID],
    baseUrl: String,
    projectId: Int,
    mainBranch: Branch,
    draftBranch: Branch,
    modulesRootFolder: String,
    coreRootFolder: String,
    moduleCompendiumRootFolder: String,
    autoApprovedLabel: String,
    reviewApprovedLabel: String
)
