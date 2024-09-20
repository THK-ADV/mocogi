package models

import git.MergeRequestId
import git.MergeRequestStatus

case class ModuleCatalogGenerationRequest(
    mergeRequestId: MergeRequestId,
    semesterId: String,
    status: MergeRequestStatus
)
