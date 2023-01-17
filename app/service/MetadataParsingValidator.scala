package service

import git.GitFilePath
import validator.Metadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// WebHookController => GitFilesDownloadActor => Broker => ModuleCompendiumPublisher (
//                                                            ModuleCompendiumParser: String -> (MetadataParserService => MetadataValidator, ContentParser) -> ModuleCompendium
//                                                            ModuleCompendiumSubscribers: ModuleCompendium -> ModuleCompendiumPrintingActor // public/ap1.html
//                                                                                                             ModuleCompendiumPublishActor // kafka json stream
//                                                                                                             MetadataDatabaseActor // ModuleCompendium => Metadata => DB
//                                                         )
//                                                      => CoreSubscriber

// Broker
//  Map[String, List[ParsingValidator]]
//  ModuleCompendiumParser: ParsingValidator
//  CoreDataParser: ParsingValidator
// ParsingValidator[A]
//   parser: Parser[A]
//   validator: Validator[A]
//   parse(input: String): Future[Validation[A]]
// WebHookController => GitFilesDownloadActor => Broker => ModuleCompendiumParsingValidator => List[ModuleCompendiumSubscriber]
//                                                      => CoreDataParsingValidator         => List[CoreDataSubscriber]

trait MetadataParsingValidator {
  def parse(input: String, gitFilePath: GitFilePath): Future[(Metadata, String)]
}

@Singleton
final class MetadataParsingValidatorImpl @Inject() (
    val parserService: MetadataParserService,
    val validatorService: MetadataValidatorService,
    val service: ModuleCompendiumService,
    private implicit val ctx: ExecutionContext
) extends MetadataParsingValidator {
  override def parse(input: String, gitFilePath: GitFilePath) =
    for {
      (parsedMetadata, rest) <- parserService.parse(input)
      metadata <- validatorService.validate(parsedMetadata)
    } yield (metadata, rest)
}
