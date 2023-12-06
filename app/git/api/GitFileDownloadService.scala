package git.api

import com.google.inject.Inject
import database._
import git.{GitConfig, GitFileContent, GitFilePath}
import models.{Branch, Module}
import ops.EitherOps.{EOps, EThrowableOps}
import ops.FutureOps.EitherOps
import parsing.types.{ModuleCompendium, ParsedModuleRelation}
import printing.PrintingLanguage
import printing.html.ModuleCompendiumHTMLPrinter
import printing.pandoc.{PrinterOutput, PrinterOutputType}
import service._
import validator.{ValidationError, Workload}

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileDownloadApiService,
    private val parser: MetadataParsingService,
    private val printer: ModuleCompendiumHTMLPrinter,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleFromDraftBranch(
      id: UUID
  ): Future[Option[ModuleCompendiumOutput]] = {
    val path = GitFilePath(id)
    for {
      content <- downloadFileContent(path, Branch(config.draftBranch))
      res <- content match {
        case Some(content) =>
          parser.parse(Print(content.value)).flatMap {
            case Right((metadata, de, en)) =>
              Future.successful(
                Some(
                  ModuleCompendiumOutput(
                    path.value,
                    MetadataOutput(
                      metadata.id,
                      metadata.title,
                      metadata.abbrev,
                      metadata.kind.abbrev,
                      metadata.credits.fold(
                        identity,
                        _.foldLeft(0.0) { case (acc, e) => acc + e.ectsValue }
                      ),
                      metadata.language.abbrev,
                      metadata.duration,
                      metadata.season.abbrev,
                      Workload(
                        metadata.workload.lecture,
                        metadata.workload.seminar,
                        metadata.workload.practical,
                        metadata.workload.exercise,
                        metadata.workload.projectSupervision,
                        metadata.workload.projectWork,
                        0,
                        0
                      ),
                      metadata.status.abbrev,
                      metadata.location.abbrev,
                      metadata.participants,
                      metadata.relation.map {
                        case ParsedModuleRelation.Parent(children) =>
                          ModuleRelationOutput.Parent(children)
                        case ParsedModuleRelation.Child(parent) =>
                          ModuleRelationOutput.Child(parent)
                      },
                      metadata.responsibilities.moduleManagement.map(_.id),
                      metadata.responsibilities.lecturers.map(_.id),
                      AssessmentMethodsOutput(
                        metadata.assessmentMethods.mandatory.map(a =>
                          AssessmentMethodEntryOutput(
                            a.method.abbrev,
                            a.percentage,
                            a.precondition.map(_.abbrev)
                          )
                        ),
                        metadata.assessmentMethods.optional.map(a =>
                          AssessmentMethodEntryOutput(
                            a.method.abbrev,
                            a.percentage,
                            a.precondition.map(_.abbrev)
                          )
                        )
                      ),
                      PrerequisitesOutput(
                        metadata.prerequisites.recommended.map(e =>
                          PrerequisiteEntryOutput(
                            e.text,
                            e.modules,
                            e.studyPrograms.map(_.abbrev)
                          )
                        ),
                        metadata.prerequisites.required.map(e =>
                          PrerequisiteEntryOutput(
                            e.text,
                            e.modules,
                            e.studyPrograms.map(_.abbrev)
                          )
                        )
                      ),
                      POOutput(
                        metadata.pos.mandatory.map(a =>
                          POMandatoryOutput(
                            a.po.abbrev,
                            a.specialization.map(_.abbrev),
                            a.recommendedSemester,
                            a.recommendedSemesterPartTime
                          )
                        ),
                        metadata.pos.optional.map(a =>
                          POOptionalOutput(
                            a.po.abbrev,
                            a.specialization.map(_.abbrev),
                            a.instanceOf,
                            a.partOfCatalog,
                            a.recommendedSemester
                          )
                        )
                      ),
                      metadata.competences.map(_.abbrev),
                      metadata.globalCriteria.map(_.abbrev),
                      metadata.taughtWith
                    ),
                    de.normalize(),
                    en.normalize()
                  )
                )
              )
            case Left(value) => Future.failed(value)
          }
        case None =>
          Future.successful(None)
      }
    } yield res
  }

  def downloadFileContent(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[GitFileContent]] =
    api.download(path, branch)

  def downloadModuleFromDraftBranchAsHTML(
      id: UUID,
      existingModules: Seq[Module]
  )(implicit lang: PrintingLanguage): Future[Option[String]] = {
    val path = GitFilePath(id)
    for {
      content <- downloadFileContent(path, Branch(config.draftBranch))
      res <- content match {
        case Some(content) =>
          for {
            (metadata, de, en) <- parser.parse(Print(content.value)).unwrap
            metadata <- MetadataValidatingService
              .validate(existingModules, metadata)
              .mapErr(errs =>
                PipelineError
                  .Validator(ValidationError(errs), Some(metadata.id))
              )
              .toFuture()
            output <- printer
              .print(
                ModuleCompendium(metadata, de, en),
                lang,
                None,
                PrinterOutputType.HTMLStandalone
              )
            res <- output match {
              case Left(err)                          => Future.failed(err)
              case Right(PrinterOutput.Text(c, _, _)) => Future.successful(c)
              case Right(PrinterOutput.File(_, _)) =>
                Future.failed(
                  new Throwable("expected standalone HTML, but was a file")
                )
            }
          } yield Some(res)
        case None =>
          Future.successful(None)
      }
    } yield res
  }
}
