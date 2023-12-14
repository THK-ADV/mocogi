package service

import database._
import git.GitFilePath
import ops.EitherOps.{EOps, EThrowableOps}
import ops.FutureOps.EitherOps
import parsing.types.{ModuleCompendium, ParsedModuleRelation}
import validator.{ValidationError, Workload}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataPipeline @Inject() (
    private val parser: MetadataParsingService,
    private val moduleCompendiumService: ModuleCompendiumService,
    implicit val ctx: ExecutionContext
) {
  def parse(print: Print, path: GitFilePath): Future[ModuleCompendiumOutput] =
    parser.parse(print).flatMap {
      case Right((metadata, de, en)) =>
        Future.successful(
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
      case Left(value) => Future.failed(value)
    }

  def parseValidate(print: Print): Future[ModuleCompendium] =
    for {
      (metadata, de, en) <- parser.parse(print).unwrap
      existing <- moduleCompendiumService.allModules(Map.empty)
      metadata <- MetadataValidatingService
        .validate(existing, metadata)
        .mapErr(errs =>
          PipelineError
            .Validator(ValidationError(errs), Some(metadata.id))
        )
        .toFuture
    } yield ModuleCompendium(metadata, de, en)

  def parseValidateMany(
      prints: Seq[(Option[UUID], Print)]
  ): Future[Either[Seq[PipelineError], Seq[(Print, ModuleCompendium)]]] =
    for {
      parsed <- parser.parseMany(prints)
      existing <- moduleCompendiumService.allModules(Map.empty)
    } yield parsed match {
      case Left(value) => Left(value)
      case Right(parsed) =>
        MetadataValidatingService.validateMany(existing, parsed)
    }
}
