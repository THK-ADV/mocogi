package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.types.ParsedModuleRelation
import parsing.{multipleValueParser, uuidParser}

import java.util.UUID

object ModuleRelationParser {

  def raw: Parser[Option[Either[UUID, List[UUID]]]] = {
    def go: Parser[Either[UUID, List[UUID]]] = oneOf(
      prefix("parent:")
        .skip(zeroOrMoreSpaces)
        .skip(prefix("module."))
        .take(prefixTo("\n") or rest)
        .flatMap(uuidParser)
        .map(Left.apply),
      multipleValueParser(
        "children",
        skipFirst(prefix("module.")).take(prefixTo("\n")).flatMap(uuidParser),
        1
      ).map(Right.apply)
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
