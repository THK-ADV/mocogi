package catalog

import git.Branch

case class ModuleCatalogConfig(
    tmpFolderPath: String,
    moduleCatalogOutputFolderPath: String,
    moduleCatalogLabel: String,
    moduleCatalogGitPath: String,
    mainBranch: Branch
)
