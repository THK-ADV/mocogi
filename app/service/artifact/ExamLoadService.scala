package service.artifact

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cli.GitCLI
import database.repo.core.AssessmentMethodRepository
import models.ModuleProtocol
import play.api.Logging
import printing.csv.ExamLoadCSVPrinter
import printing.csv.MandatoryModule

final class ExamLoadService @Inject() (
    assessmentMethodRepo: AssessmentMethodRepository,
    gitCli: GitCLI,
    implicit val ctx: ExecutionContext
) extends Logging {

  /**
   * Returns default mandatory modules sorted by recommended semester and module title.
   *
   * Specialization-specific modules are excluded because exam loads do not support
   * assigning modules to PO specializations yet.
   */
  private def prepareModules(modules: Vector[ModuleProtocol], poId: String): Vector[MandatoryModule] =
    modules
      .flatMap { module =>
        module.metadata.po.mandatory.filter(po => po.po == poId && po.specialization.isEmpty) match {
          case po :: Nil =>
            Some(
              MandatoryModule(
                module.id.get,
                module.metadata,
                po.recommendedSemester.sorted
              )
            )
          case _ => None
        }
      }
      .sortBy(module => (module.semesters.headOption.getOrElse(Int.MaxValue), module.metadata.title))

  /**
   * Returns the latest exam load for the given PO as a CSV string using the preview branch
   */
  def createLatestExamLoad(po: String): Future[String] =
    for {
      poModules <- Future.fromTry(new ModulePreview(gitCli).getByPO(po))
      modules   = prepareModules(poModules.modules.map(_._1), po)
      moduleIds = (modules.map(_.id) ++ modules.flatMap(_.metadata.childIds)).distinct
      assessmentMethods <- assessmentMethodRepo.allPermittedLabelsForModulesOrDefault(moduleIds)
    } yield new ExamLoadCSVPrinter(modules, poModules.childrenById, assessmentMethods).print()
}
