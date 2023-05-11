package models

case class UserBranch(
    user: String,
    branch: String,
    commitId: Option[String],
    mergeRequestId: Option[Int]
)
