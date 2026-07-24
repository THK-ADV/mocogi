package parsing.metadata

import java.util.UUID

import cats.data.NonEmptyList
import parser.Parser
import parser.Parser._
import parser.ParserOps.P0
import parsing.multipleValueParser
import parsing.nel
import parsing.uuidParser

object ModuleRelationParser {

  /**
   * Parses the children of a parent module. Legacy `parent:` declarations of child modules are
   * ignored: the relation is only stored on the parent since [[models.ModuleRelationProtocol]].
   */
  def parser: Parser[Option[NonEmptyList[UUID]]] = {
    val children =
      multipleValueParser(
        "children",
        skipFirst(prefix("module.")).take(prefixTo("\n")).flatMap(uuidParser)
      ).nel()

    val ignoredChildRelation =
      prefix("parent:")
        .take(prefixTo("\n").or(rest))
        .map(_ => Option.empty[NonEmptyList[UUID]])

    val relation = oneOf(
      children.map(Some.apply),
      ignoredChildRelation
    )

    prefix("relation:")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(relation)
      .option
      .map(_.flatten)
  }
}
