package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.intForKey

object ParticipantsParser {
  case class Participants(min: Int, max: Int)

  val participantsParser: Parser[Participants] =
    skipFirst(prefix("participants:"))
      .skip(zeroOrMoreSpaces)
      .take(intForKey("min"))
      .skip(zeroOrMoreSpaces)
      .zip(intForKey("max"))
      .flatMap { case (min, max) =>
        if (min <= max) always(Participants(min, max))
        else never(s"min $min should be lower than max $max")
      }
}