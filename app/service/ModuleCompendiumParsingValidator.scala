package service

import git.GitFilePath
import parsing.types.ModuleCompendium

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleCompendiumParsingValidator {
  def parse(
      input: String,
      gitFilePath: GitFilePath
  ): Future[(ModuleCompendium, String)]
}

@Singleton
final class ModuleCompendiumParsingValidatorImpl @Inject() (
    private val metadataParsingValidator: MetadataParsingValidator,
    private val moduleCompendiumContentParsing: ModuleCompendiumContentParsing,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumParsingValidator {
  def parse(
      input: String,
      gitFilePath: GitFilePath
  ): Future[(ModuleCompendium, String)] =
    for {
      (metadata, rest) <- metadataParsingValidator.parse(input, gitFilePath)
      ((de, en), rest2) <- moduleCompendiumContentParsing.parse(rest)
    } yield (ModuleCompendium(metadata, de, en), rest2)
}
