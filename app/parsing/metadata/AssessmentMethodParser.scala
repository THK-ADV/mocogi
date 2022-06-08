package parsing.metadata

import parser.Parser
import parser.Parser._
import parsing.helper.{MultipleValueParser, SimpleFileParser}
import parsing.types.AssessmentMethod

object AssessmentMethodParser
    extends SimpleFileParser[AssessmentMethod]
    with MultipleValueParser[AssessmentMethod] {

  override val makeType = AssessmentMethod.tupled
  override val filename = "assessment.yaml"
  override val typename = "assessment methods"

  val assessmentMethodFileParser: Parser[List[AssessmentMethod]] = fileParser

  val assessmentMethods: List[AssessmentMethod] = types

  val assessmentMethodParser: Parser[List[AssessmentMethod]] =
    multipleParser(
      "assessment-methods",
      oneOf(
        assessmentMethods.map(s =>
          literal(s"assessment.${s.abbrev}")
            .skip(newline)
            .map(_ => s)
        )
      )
    )
}
