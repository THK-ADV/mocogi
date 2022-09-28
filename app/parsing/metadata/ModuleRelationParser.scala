package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.types.ModuleRelation

object ModuleRelationParser {

  val moduleRelationParser: Parser[Option[ModuleRelation]] = {
    def go: Parser[ModuleRelation] = oneOf(
      prefix("parent:")
        .skip(zeroOrMoreSpaces)
        .skip(prefix("module."))
        .take(prefixTo("\n") or rest)
        .map[ModuleRelation](ModuleRelation.Child.apply),
      prefix("children:")
        .skip(newline)
        .take(
          zeroOrMoreSpaces
            .skip(prefix("-"))
            .skip(zeroOrMoreSpaces)
            .skip(prefix("module."))
            .take(prefixUntil("\n") or rest)
            .many(newline, minimum = 1)
        )
        .map[ModuleRelation](ModuleRelation.Parent.apply)
    )

    prefix("relation:")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(go)
      .option
  }
}
