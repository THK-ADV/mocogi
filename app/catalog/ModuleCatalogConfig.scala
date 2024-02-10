package catalog

import models.Branch

case class ModuleCatalogConfig(
    tmpFolderPath: String,
    moduleCatalogFolderPath: String,
    repoPath: String,
    mcPath: String,
    pushScriptPath: String,
    mainBranch: Branch,
    moduleCatalogLabel: String
)
