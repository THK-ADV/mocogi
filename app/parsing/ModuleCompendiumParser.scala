package parsing

import parser.Parser
import parser.Parser.newline
import parsing.content.ContentParser.contentParser
import parsing.metadata.MetadataParser
import parsing.types.ModuleCompendium

import javax.inject.{Inject, Singleton}

trait ModuleCompendiumParser {
  val parser: Parser[ModuleCompendium]
}

@Singleton
class ModuleCompendiumParserImpl @Inject() (metadataParser: MetadataParser)
    extends ModuleCompendiumParser {
  val parser: Parser[ModuleCompendium] =
    metadataParser.parser
      .skip(newline.many())
      .zip(contentParser)
      .map(a => ModuleCompendium(a._1, a._2._1, a._2._2))
}
