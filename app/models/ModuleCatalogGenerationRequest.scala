package models

case class ModuleCatalogGenerationRequest(
    mergeRequestId: MergeRequestId,
    semesterId: String,
    status: MergeRequestStatus
)
