package parsing.core

import models.core.Specialization
import parser.Parser.{prefixTo, zeroOrMoreSpaces}
import parser.ParserOps.P2
import parsing.{singleLineStringForKey, singleValueParser}

object SpecializationFileParser {
  def fileParser(implicit pos: Seq[String]) =
    prefixTo(":")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("label"))
      .skip(zeroOrMoreSpaces)
      .take(
        singleValueParser[String]("po", p => s"po.$p")(pos.sorted.reverse)
      )
      .skip(zeroOrMoreSpaces)
      .map(Specialization.tupled)
      .all()
}
