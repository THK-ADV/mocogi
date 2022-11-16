package service

import git.GitFilePath
import ops.PrettyPrinter
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


trait  MetadataPipeline {
  def go(input: String, gitFilePath: GitFilePath): Future[Metadata]
}

@Singleton
final class MetadataPipelineImpl @Inject() (
    val parserService: MetadataParserService, // TODO ModuleCompendium
    val validatorService: MetadataValidatorService,
    val service: MetadataService,
    private implicit val ctx: ExecutionContext
) extends MetadataPipeline {
  override def go(input: String, gitFilePath: GitFilePath) =
    for {
      parsedMetadata <- parserService.parse(input)
      validation <- validatorService.validate(parsedMetadata)
      metadata <- validation.fold(
        errs =>
          Future.failed(
            new Throwable(
              s"Validation failed with Errors: ${errs.mkString("\n")}"
            )
          ),
        Future.successful
      )
      created <- service.createOrUpdate(metadata, gitFilePath) // TODO remove this step from the pipeline. writing into database is part of the pub/sub architecture
      _ = println(PrettyPrinter.prettyPrint(created))
    } yield created
}
