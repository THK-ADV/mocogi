package parsing.validator

import monocle.Lens

private[parsing] final class DegreeValidator[A](
    degrees: Seq[String],
    degree: Lens[A, String]
) extends YamlFileParserValidator[A] {
  override def expected(): String = degrees.mkString(", ")

  override def validate(a: A): Either[String, A] = {
    val degree = this.degree.get(a).stripPrefix("grade.")
    Either.cond(
      degrees.contains(degree),
      this.degree.replace(degree).apply(a),
      this.degree.get(a)
    )
  }
}
