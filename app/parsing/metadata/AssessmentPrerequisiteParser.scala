package parsing.metadata

import models.AssessmentPrerequisite
import parser.Parser
import parser.Parser.prefixTo
import parser.Parser.skipFirst
import parser.Parser.zeroOrMoreSpaces
import parser.ParserOps.P0
import parsing.singleLineStringForKey

object AssessmentPrerequisiteParser {
  def key        = "assessment_prerequisite"
  def modulesKey = "modules"
  def reasonKey  = "reason"

  def parser: Parser[AssessmentPrerequisite] =
    skipFirst(prefixTo(s"$key:").skip(zeroOrMoreSpaces))
      .take(singleLineStringForKey(modulesKey))
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey(reasonKey))
      .map(AssessmentPrerequisite.apply)
}
