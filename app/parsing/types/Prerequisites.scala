package parsing.types

case class Prerequisites (
  recommended: Option[PrerequisiteEntry],
  required: Option[PrerequisiteEntry],
)
