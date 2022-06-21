package parsing

import parser.Parser
import parser.Parser.newline
import parsing.content.ContentParser.contentParser
import parsing.metadata.MetadataParser.metadataParser
import parsing.types.ModuleCompendium

object ModuleCompendiumParser {
  val moduleCompendiumParser: Parser[ModuleCompendium] =
    metadataParser
      .skip(newline.many())
      .zip(contentParser)
      .map(a => ModuleCompendium(a._1, a._2._1, a._2._2))
}
