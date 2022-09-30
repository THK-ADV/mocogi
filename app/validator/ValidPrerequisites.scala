package validator

case class ValidPrerequisites (
  recommended: Option[ValidPrerequisiteEntry],
  required: Option[ValidPrerequisiteEntry],
)
