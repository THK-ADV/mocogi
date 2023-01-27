package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.types.ParsedModuleRelation
import parsing.uuidParser

object ModuleRelationParser {

  val moduleRelationParser: Parser[Option[ParsedModuleRelation]] = {
    def go: Parser[ParsedModuleRelation] = oneOf(
      prefix("parent:")
        .skip(zeroOrMoreSpaces)
        .skip(prefix("module."))
        .take(prefixTo("\n") or rest)
        .flatMap(uuidParser)
        .map[ParsedModuleRelation](ParsedModuleRelation.Child.apply),
      prefix("children:")
        .skip(newline)
        .take(
          zeroOrMoreSpaces
            .skip(prefix("-"))
            .skip(zeroOrMoreSpaces)
            .skip(prefix("module."))
            .take(prefixUntil("\n") or rest)
            .flatMap(uuidParser)
            .many(newline, minimum = 1)
        )
        .map[ParsedModuleRelation](ParsedModuleRelation.Parent.apply)
    )

    prefix("relation:")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(go)
      .option
  }
}
