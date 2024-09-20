package parsing.validator

import models.core.Identity

final class FacultyValidator(
    faculties: Seq[String]
) extends YamlFileParserValidator[Identity] {
  override def expected(): String = faculties.mkString(", ")

  override def validate(i: Identity): Either[String, Identity] =
    i match
      case p: Identity.Person =>
        val (good, bad) = p.faculties
          .map(_.stripPrefix("faculty."))
          .partition(this.faculties.contains)
        Either.cond(bad.isEmpty, p.copy(faculties = good), bad.mkString(", "))
      case Identity.Group(_, _)   => Right(i)
      case Identity.Unknown(_, _) => Right(i)
}
