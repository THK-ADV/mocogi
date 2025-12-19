package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.intForKey
import parsing.types.ModuleParticipants

object ModuleParticipantsParser {

  def key = "participants"

  def minKey = "min"

  def maxKey = "max"

  private[parsing] def parser: Parser[ModuleParticipants] =
    skipFirst(prefix(s"$key:"))
      .skip(zeroOrMoreSpaces)
      .take(intForKey(minKey))
      .skip(zeroOrMoreSpaces)
      .zip(intForKey(maxKey))
      .flatMap {
        case (min, max) =>
          if (min <= max) always(ModuleParticipants(min, max))
          else never(s"min $min should be lower than max $max")
      }
}
