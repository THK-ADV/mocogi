package service.core

import java.util.UUID
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import database.repo.core.AssessmentMethodRepository
import database.repo.PermittedAssessmentMethodForModuleRepository
import models.core.AssessmentMethod
import models.AssessmentMethodSource
import models.PermittedAssessmentMethodForModule

@Singleton
final class AssessmentMethodService @Inject() (
    repo: AssessmentMethodRepository,
    moduleAssessmentMethodRepo: PermittedAssessmentMethodForModuleRepository,
    implicit val ctx: ExecutionContext
) {
  def all(): Future[Seq[AssessmentMethod]] =
    repo.all()

  def allRPO(): Future[Seq[AssessmentMethod]] =
    repo.allBySource(AssessmentMethodSource.RPO)

  def allForModule(module: UUID): Future[Seq[AssessmentMethod]] =
    for
      allByModule <- moduleAssessmentMethodRepo.allByModule(module)
      res         <- if allByModule.isEmpty then allRPO() else Future.successful(allByModule)
    yield res

  def create(xs: List[PermittedAssessmentMethodForModule]): Future[Option[Int]] =
    moduleAssessmentMethodRepo.insert(xs)
}
