package models

import git.{MergeRequestId, MergeRequestStatus}

case class ModuleCatalogGenerationRequest(
    mergeRequestId: MergeRequestId,
    semesterId: String,
    status: MergeRequestStatus
)
