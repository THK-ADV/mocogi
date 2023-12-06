package git.api

import com.google.inject.Inject
import database._
import git.{GitConfig, GitFileContent, GitFilePath}
import models.Branch
import parsing.types.ParsedModuleRelation
import service.{MetadataParsingService, Print}
import validator.Workload

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileDownloadApiService,
    private val parser: MetadataParsingService,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleFromDraftBranch(
      id: UUID
  ): Future[Option[ModuleCompendiumOutput]] =
    downloadModule(id, Branch(config.draftBranch))

  def downloadFileContent(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[GitFileContent]] =
    api.download(path, branch)

  def downloadModule(
      id: UUID,
      branch: Branch
  ): Future[Option[ModuleCompendiumOutput]] = {
    val path = GitFilePath(id)
    for {
      content <- downloadFileContent(path, branch)
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
}
