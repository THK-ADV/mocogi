package service

import git.GitFilePath
import ops.PrettyPrinter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataPipeline {
  def go(input: String): Future[Unit]
}

@Singleton
final class MetadataPipelineImpl @Inject() (
    val service: MetadataService,
    val parserService: MetadataParserService,
    val validatorService: MetadataValidatorService,
    private implicit val ctx: ExecutionContext
) extends MetadataPipeline {
  override def go(input: String) =
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
      created <- service.create(
        metadata,
        GitFilePath(metadata.abbrev.toLowerCase) // TODO
      )
      _ = println(PrettyPrinter.prettyPrint(created))
    } yield ()
}
