package service

import models._
import ops.EitherOps.{EOps, EThrowableOps}
import ops.FutureOps.EitherOps
import parsing.metadata.VersionScheme
import parsing.types.{
  Module,
  ModuleContent,
  ParsedMetadata,
  ParsedModuleRelation
}
import printing.yaml.ModuleYamlPrinter
import validator.{Metadata, ModuleWorkload, ValidationError}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataPipeline @Inject() (
    private val parser: MetadataParsingService,
    private val moduleService: ModuleService,
    private val moduleYamlPrinter: ModuleYamlPrinter,
    implicit val ctx: ExecutionContext
) {

  // TODO implement raw parsing and use it where needed
  def parseRaw(print: Print) = ???

  def parse(print: Print): Future[ModuleProtocol] =
    parser.parse(print).flatMap {
      case Right((metadata, de, en)) =>
        Future.successful(
          ModuleProtocol(
            Some(metadata.id),
            MetadataProtocol(
              metadata.title,
              metadata.abbrev,
              metadata.kind.id,
              metadata.credits.fold(
                identity,
                _.foldLeft(0.0) { case (acc, e) => acc + e.ectsValue }
              ),
              metadata.language.id,
              metadata.duration,
              metadata.season.id,
              ModuleWorkload(
                metadata.workload.lecture,
                metadata.workload.seminar,
                metadata.workload.practical,
                metadata.workload.exercise,
                metadata.workload.projectSupervision,
                metadata.workload.projectWork,
                0,
                0
              ),
              metadata.status.id,
              metadata.location.id,
              metadata.participants,
              metadata.relation.map {
                case ParsedModuleRelation.Parent(children) =>
                  ModuleRelationProtocol.Parent(children)
                case ParsedModuleRelation.Child(parent) =>
                  ModuleRelationProtocol.Child(parent)
              },
              metadata.responsibilities.moduleManagement.map(_.id),
              metadata.responsibilities.lecturers.map(_.id),
              ModuleAssessmentMethodsProtocol(
                metadata.assessmentMethods.mandatory.map(a =>
                  ModuleAssessmentMethodEntryProtocol(
                    a.method.id,
                    a.percentage,
                    a.precondition.map(_.id)
                  )
                ),
                metadata.assessmentMethods.optional.map(a =>
                  ModuleAssessmentMethodEntryProtocol(
                    a.method.id,
                    a.percentage,
                    a.precondition.map(_.id)
                  )
                )
              ),
              ModulePrerequisitesProtocol(
                metadata.prerequisites.recommended.map(e =>
                  ModulePrerequisiteEntryProtocol(
                    e.text,
                    e.modules,
                    e.studyPrograms.map(_.id)
                  )
                ),
                metadata.prerequisites.required.map(e =>
                  ModulePrerequisiteEntryProtocol(
                    e.text,
                    e.modules,
                    e.studyPrograms.map(_.id)
                  )
                )
              ),
              ModulePOProtocol(
                metadata.pos.mandatory.map(a =>
                  ModulePOMandatoryProtocol(
                    a.po.id,
                    a.specialization.map(_.id),
                    a.recommendedSemester
                  )
                ),
                metadata.pos.optional.map(a =>
                  ModulePOOptionalProtocol(
                    a.po.id,
                    a.specialization.map(_.id),
                    a.instanceOf,
                    a.partOfCatalog,
                    a.recommendedSemester
                  )
                )
              ),
              metadata.competences.map(_.id),
              metadata.globalCriteria.map(_.id),
              metadata.taughtWith
            ),
            de.normalize(),
            en.normalize()
          )
        )
      case Left(value) => Future.failed(value)
    }

  def parseValidate(print: Print): Future[Module] =
    for {
      (metadata, de, en) <- parser.parse(print).unwrap
      existing <- moduleService.allModuleCore(Map.empty)
      metadata <- MetadataValidatingService
        .validate(existing, metadata)
        .mapErr(errs =>
          PipelineError
            .Validator(ValidationError(errs), Some(metadata.id))
        )
        .toFuture
    } yield Module(metadata, de, en)

  def parseValidateMany(
      prints: Seq[(Option[UUID], Print)]
  ): Future[Either[Seq[PipelineError], Seq[(Print, Module)]]] =
    for {
      parsed <- parser.parseMany(prints)
      existing <- moduleService.allModuleCore(Map.empty)
    } yield parsed match {
      case Left(value) => Left(value)
      case Right(parsed) =>
        MetadataValidatingService.validateMany(existing, parsed)
    }

  def printParseValidate(
      protocol: ModuleProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Future[Either[PipelineError, (Module, Print)]] = {
    def print(): Either[PipelineError, Print] =
      moduleYamlPrinter
        .print(versionScheme, moduleId, protocol)
        .bimap(
          PipelineError.Printer(_, Some(moduleId)),
          Print.apply
        )

    def parse(
        print: Print
    ): Future[
      Either[PipelineError, (ParsedMetadata, ModuleContent, ModuleContent)]
    ] =
      parser
        .parse(print)
        .map(_.bimap(PipelineError.Parser(_, Some(moduleId)), identity))

    def validate(
        metadata: ParsedMetadata
    ): Future[Either[PipelineError, Metadata]] =
      moduleService.allModuleCore(Map.empty).map { existing =>
        MetadataValidatingService
          .validate(existing, metadata)
          .bimap(
            errs =>
              PipelineError
                .Validator(ValidationError(errs), Some(metadata.id)),
            identity
          )
      }

    for {
      parsed <- continueWith(print())(parse)
      validated <- continueWith(parsed)(a => validate(a._2._1))
    } yield validated.map(t => (Module(t._2, t._1._2._2, t._1._2._3), t._1._1))
  }
}
