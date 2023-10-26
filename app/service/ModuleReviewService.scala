package service

import database.repo.ModuleReviewRepository
import models.{
  ModuleReview,
  ModuleReviewRequest,
  ModuleReviewStatus,
  UniversityRole
}
import service.core.StudyProgramService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewService @Inject() (
    private val reviewRepository: ModuleReviewRepository,
    private val studyProgramService: StudyProgramService,
    private implicit val ctx: ExecutionContext
) {

  def create(
      moduleDraft: UUID,
      requiredRoles: Set[UniversityRole],
      affectedPOs: Set[String]
  ): Future[ModuleReview] =
    for {
      directors <- studyProgramService.allDirectorsFromPOs(
        affectedPOs,
        requiredRoles
      )
      res <- reviewRepository.create(
        ModuleReview(
          moduleDraft,
          ModuleReviewStatus.WaitingForApproval,
          directors
            .map(p =>
              ModuleReviewRequest(
                moduleDraft,
                p.person,
                ModuleReviewRequest.Pending
              )
            )
            .distinctBy(r => (r.review, r.reviewer))
        )
      )
    } yield res

  def delete(moduleId: UUID) =
    reviewRepository.delete(moduleId)

  def deleteMany(moduleIds: List[UUID]) =
    Future.sequence(moduleIds.map(delete))
}
