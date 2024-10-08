package parsing.metadata

import cats.data.NonEmptyList
import models.core.ExamPhases.ExamPhase
import parser.Parser
import parsing.multipleValueParser
import parsing.multipleValueRawParser
import parsing.ParserListOps

object ExamPhaseParser {
  def key    = "exam_phases"
  def prefix = "exam_phase."

  def parser(implicit phases: Seq[ExamPhase]): Parser[NonEmptyList[ExamPhase]] =
    multipleValueParser(
      key,
      (p: ExamPhase) => s"$prefix${p.id}"
    )(phases.sortBy(_.id).reverse).option
      .map(_.getOrElse(List(ExamPhase.none)))
      .nel()

  def raw: Parser[NonEmptyList[String]] =
    multipleValueRawParser(key, prefix).option
      .map(_.getOrElse(List(ExamPhase.none.id)))
      .nel()
}
