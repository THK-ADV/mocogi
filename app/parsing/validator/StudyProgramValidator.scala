package parsing.validator

import monocle.Lens

final class StudyProgramValidator[A](
    programs: Seq[String],
    program: Lens[A, String]
) extends YamlFileParserValidator[A] {
  override def expected(): String = programs.mkString(", ")

  override def validate(a: A): Either[String, A] = {
    val program = this.program.get(a).stripPrefix("program.")
    Either.cond(
      programs.contains(program),
      this.program.replace(program).apply(a),
      this.program.get(a)
    )
  }
}
