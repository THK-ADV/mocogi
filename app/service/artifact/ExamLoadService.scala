package service.artifact

import javax.inject.Inject

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cli.GitCLI
import models.ModuleProtocol
import models.ModuleRelationProtocol
import play.api.Logging
import printing.csv.ExamLoadCSVPrinter
import printing.csv.Module
import service.AssessmentMethodService

final class ExamLoadService @Inject() (
    assessmentMethodService: AssessmentMethodService,
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
   * Returns all modules from the PO sorted by recommended semester
   */
  private def prepareModules(modules: Vector[ModuleProtocol], po: String): Vector[Module] = {
    modules
      .map(m =>
        Module(
          m.id.get,
          m.metadata,
          (m.metadata.po.mandatory.flatMap(_.recommendedSemester) ::: m.metadata.po.optional
            .flatMap(_.recommendedSemester)).distinct.sorted
        )
      )
      .sortBy { m =>
        val title = m.metadata.title
        if m.semester.isEmpty then (Int.MaxValue, title)
        else (m.semester.head, title) // head is safe because it's sorted
      }
  }

  /**
   * Returns all modules from the PO sorted by recommended semester
   */
  private def prepareChildren(children: Vector[ModuleProtocol], modules: Vector[Module]): Vector[ModuleProtocol] =
    children.filter(child => modules.exists(m => child.metadata.moduleRelation.exists(_.parentID.contains(m.id))))

  /**
   * Returns the latest exam load for the given PO as a CSV string using the preview branch
   */
  def createLatestExamLoad(po: String): Future[String] = {
    val assessmentMethods               = assessmentMethodService.all()
    val (parsedModules, parsedChildren) = getModulesFromPreview(po)
    val modules                         = prepareModules(parsedModules, po)
    val children                        = prepareChildren(parsedChildren, modules)
    for assessmentMethods <- assessmentMethods
    yield new ExamLoadCSVPrinter(modules, children, po, assessmentMethods).print()
  }
}
