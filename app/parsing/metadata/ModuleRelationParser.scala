package parsing.metadata

import java.util.UUID

import cats.data.NonEmptyList
import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.multipleValueParser
import parsing.types.ParsedModuleRelation
import parsing.uuidParser
import parsing.ParserListOps

object ModuleRelationParser {

  def raw: Parser[Option[Either[UUID, NonEmptyList[UUID]]]] = {
    def go: Parser[Either[UUID, NonEmptyList[UUID]]] = oneOf(
      prefix("parent:")
        .skip(zeroOrMoreSpaces)
        .skip(prefix("module."))
        .take(prefixTo("\n").or(rest))
        .flatMap(uuidParser)
        .map(Left.apply),
      multipleValueParser(
        "children",
        skipFirst(prefix("module.")).take(prefixTo("\n")).flatMap(uuidParser)
      )
        .nel()
        .map(Right.apply)
    )

    prefix("relation:")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(go)
      .option
  }

  def parser: Parser[Option[ParsedModuleRelation]] =
    raw.map(
      _.map(
        _.fold(
          ParsedModuleRelation.Child.apply,
          ParsedModuleRelation.Parent.apply
        )
      )
    )
}
