package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2, P3, P4, P5}
import parsing.posIntForKey
import parsing.types.ParsedWorkload

object ModuleWorkloadParser {

  def projectWorkKey = "project_work"

  def projectSupervisionKey = "project_supervision"

  def exerciseKey = "exercise"

  def practicalKey = "practical"

  def seminarKey = "seminar"

  def lectureKey = "lecture"

  def key = "workload"

  def parser: Parser[ParsedWorkload] =
    prefix(key + ":")
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey(lectureKey))
      .skip(zeroOrMoreSpaces)
      .zip(posIntForKey(seminarKey))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey(practicalKey))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey(exerciseKey))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey(projectSupervisionKey))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey(projectWorkKey))
      .map(a => ParsedWorkload(a._1, a._2, a._3, a._4, a._5, a._6))
}
