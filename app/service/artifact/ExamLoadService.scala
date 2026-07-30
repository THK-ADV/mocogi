package service.artifact

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cli.GitCLI
import database.repo.core.AssessmentMethodRepository
import models.ModuleProtocol
import play.api.Logging
import printing.csv.ElectiveGroup
import printing.csv.ExamLoadCSVPrinter
import printing.csv.ExamLoadModule

final class ExamLoadService @Inject() (
    assessmentMethodRepo: AssessmentMethodRepository,
    gitCli: GitCLI,
    implicit val ctx: ExecutionContext
) extends Logging {

  private given Ordering[ExamLoadModule] =
    Ordering.by(module => (module.semesters.headOption.getOrElse(Int.MaxValue), module.metadata.title))

  /**
   * Returns default mandatory modules sorted by recommended semester and module title.
   * Specialization-specific modules are excluded because exam loads do not support
   * assigning modules to PO specializations yet.
   */
  private def prepareMandatoryModules(modules: Vector[ModuleProtocol], poId: String): Vector[ExamLoadModule] =
    modules.flatMap { module =>
      module.metadata.po.mandatory.filter(po => po.po == poId && po.specialization.isEmpty) match {
        case po :: Nil =>
          Some(
            ExamLoadModule(
              module.id.get,
              module.metadata,
              po.recommendedSemester.sorted
            )
          )
        case _ => None
      }
    }.sorted

  /**
   * Returns elective modules grouped by their generic module, sorted by generic title.
   * Within each group, electives are sorted by recommended semester and module title.
   * Specialization-specific electives are excluded (same as mandatory).
   */
  private def prepareElectiveGroups(modules: Vector[ModuleProtocol], poId: String): Vector[ElectiveGroup] =
    modules
      .flatMap { module =>
        module.metadata.po.optional
          .filter(po => po.po == poId && po.specialization.isEmpty)
          .map(po => po.instanceOf -> ExamLoadModule(module.id.get, module.metadata, po.recommendedSemester.sorted))
      }
      .groupMap(_._1)(_._2)
      .flatMap {
        case (genericId, electives) =>
          modules.find(_.id.contains(genericId)) match {
            case Some(generic) =>
              Some(ElectiveGroup(generic.metadata.title, electives.sorted))
            case None =>
              logger.warn(
                s"Skipping ${electives.size} elective module(s) for missing generic module $genericId in PO $poId"
              )
              None
          }
      }
      .toVector
      .sortBy(_.genericTitle)

  /**
   * Returns the latest exam load for the given PO as a CSV string using the preview branch
   */
  def createLatestExamLoad(po: String): Future[String] =
    for {
      poModules <- Future.fromTry(new ModulePreview(gitCli).getByPO(po))
      modules        = poModules.modules.map(_._1)
      mandatory      = prepareMandatoryModules(modules, po)
      electiveGroups = prepareElectiveGroups(modules, po)
      printable      = mandatory ++ electiveGroups.flatMap(_.modules)
      moduleIds      = (printable.map(_.id) ++ printable.flatMap(_.metadata.childIds)).distinct
      assessmentMethods <- assessmentMethodRepo.allPermittedLabelsForModulesOrDefault(moduleIds)
    } yield new ExamLoadCSVPrinter(mandatory, poModules.childrenById, assessmentMethods, electiveGroups).print()
}
