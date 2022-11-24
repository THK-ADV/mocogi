package parsing.base

import basedata.GlobalCriteria
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2, P3, P4}
import parsing.{removeIndentation, singleLineStringForKey, stringForKey}

import javax.inject.Singleton

object GlobalCriteriaFileParser extends FileParser[GlobalCriteria] {
  override val fileParser: Parser[List[GlobalCriteria]] =
    skipFirst(removeIndentation())
      .take(
        prefixTo(":")
          .skip(newline)
          .skip(zeroOrMoreSpaces)
          .zip(singleLineStringForKey("de_label"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("de_desc").option.map(_.getOrElse("")))
          .skip(zeroOrMoreSpaces)
          .take(singleLineStringForKey("en_label"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("en_desc").option.map(_.getOrElse("")))
          .all(zeroOrMoreSpaces)
          .map(_.map(GlobalCriteria.tupled))
      )
}
