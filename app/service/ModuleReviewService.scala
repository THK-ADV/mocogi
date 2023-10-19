package service

import database.repo.{ModuleReviewRepository, ModuleReviewRequestRepository}
import models.{ModuleReview, User}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewService @Inject() (
    private val reviewRepository: ModuleReviewRepository,
    private val reviewRequestRepository: ModuleReviewRequestRepository,
    private implicit val ctx: ExecutionContext
) {

  def create(review: ModuleReview): Future[ModuleReview] =
    for {
      _ <- reviewRepository.create((review.moduleDraft, review.status))
      _ <- reviewRequestRepository.createMany(
        review.requests.map(r => (review.moduleDraft, r.reviewer, r.approved))
      )
    } yield review

  def delete(moduleId: UUID) =
    for {
      _ <- reviewRequestRepository.delete(moduleId)
      _ <- reviewRepository.delete(moduleId)
    } yield ()

  def getForUser(user: User) =
    reviewRequestRepository.allFromUser(user)
}
