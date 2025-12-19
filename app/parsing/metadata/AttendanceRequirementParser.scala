package parsing.metadata

import models.AttendanceRequirement
import parser.Parser
import parser.Parser.prefixTo
import parser.Parser.skipFirst
import parser.Parser.zeroOrMoreSpaces
import parser.ParserOps.P0
import parser.ParserOps.P2
import parsing.singleLineStringForKey

object AttendanceRequirementParser {
  def key        = "attendance_requirement"
  def minKey     = "min"
  def reasonKey  = "reason"
  def absenceKey = "absence"

  private[parsing] def parser: Parser[AttendanceRequirement] =
    skipFirst(prefixTo(s"$key:").skip(zeroOrMoreSpaces))
      .take(singleLineStringForKey(minKey))
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey(reasonKey))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey(absenceKey))
      .map(AttendanceRequirement.apply)
}
