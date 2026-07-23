package service.artifact

import javax.inject.Inject

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.NonEmptyList
import cli.GitCLI
import database.repo.core.AssessmentMethodRepository
import models.ModuleProtocol
import models.ModuleRelationProtocol
import play.api.Logging
import printing.csv.ExamLoadCSVPrinter
import printing.csv.MandatoryModule

final class ExamLoadService @Inject() (
    assessmentMethodRepo: AssessmentMethodRepository,
    gitCli: GitCLI,
    implicit val ctx: ExecutionContext
) extends Logging {

  /**
   * Returns all modules from preview (first arg) and all children (second arg)
   */
  private def getModulesFromPreview(po: String): (Vector[ModuleProtocol], Vector[ModuleProtocol]) = {
    val preview         = new ModulePreview(gitCli)
    val modulesInPO     = preview.getAllFromPreviewByPO(po)
    val childrenModules = ListBuffer[ModuleProtocol]()
    for (module <- modulesInPO) {
      module.metadata.moduleRelation.collect {
        case ModuleRelationProtocol.Parent(children) =>
          childrenModules ++= modulesInPO.filter(m => children.exists(_ == m.id.get))
      }
    }
    (modulesInPO, childrenModules.toVector.distinctBy(_.id.get))
  }

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
   * Returns all modules from the PO sorted by recommended semester
   */
  private def prepareChildren(
      children: Vector[ModuleProtocol],
      modules: Vector[MandatoryModule]
  ): Vector[ModuleProtocol] =
    children.filter(child => modules.exists(m => child.metadata.moduleRelation.exists(_.parentID.contains(m.id))))

  /**
   * Returns the latest exam load for the given PO as a CSV string using the preview branch
   */
  def createLatestExamLoad(po: String): Future[String] = {
    val (parsedModules, parsedChildren) = getModulesFromPreview(po)
    val modules                         = prepareModules(parsedModules, po)
    val children                        = prepareChildren(parsedChildren, modules)
    for assessmentMethods <- assessmentMethodRepo.allPermittedLabelsForModulesOrDefault(modules.map(_.id))
    yield new ExamLoadCSVPrinter(modules, children, assessmentMethods).print()
  }
}
