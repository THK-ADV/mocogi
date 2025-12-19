package parsing.validator

import models.core.Identity

private[parsing] final class FacultyValidator extends YamlFileParserValidator[Identity] {
  private val prefix = "faculty."

  override def expected(): String = s"prefix '$prefix'"

  override def validate(i: Identity): Either[String, Identity] =
    i match
      case p: Identity.Person =>
        val (bad, good) =
          p.faculties.partitionMap(f => if f.startsWith(prefix) then Right(f.stripPrefix(prefix)) else Left(f))
        Either.cond(bad.isEmpty, p.copy(faculties = good), bad.mkString(", "))
      case Identity.Group(_, _)   => Right(i)
      case Identity.Unknown(_, _) => Right(i)
}
