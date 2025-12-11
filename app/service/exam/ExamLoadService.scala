package service.exam

import java.util.UUID
import javax.inject.Inject

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.cli.ModuleGitCLI
import models.*
import models.core.AssessmentMethod
import play.api.Logging
import service.AssessmentMethodService
import service.ModulePreview

/*
  This feature is currently experimental. Feedback is expected for further development.
 */

case class Module(id: UUID, metadata: MetadataProtocol, semester: List[Int])

case class Row(
    semester: String,
    module: String,
    moduleNumber: String,
    submodule: String,
    moduleType: String,
    submoduleCredits: String,
    totalCredits: String,
    attendanceRequirement: String,
    attendanceRequirementText: String,
    attendanceRequirementJustification: String,
    assessmentPrerequisite: String,
    assessmentPrerequisiteText: String,
    assessmentPrerequisiteJustification: String,
    assessmentMethods: String,
    assessmentMethodsCount: String
) {
  def toList: List[String] =
    List(
      semester,
      module,
      moduleNumber,
      submodule,
      moduleType,
      submoduleCredits,
      totalCredits,
      attendanceRequirement,
      attendanceRequirementText,
      attendanceRequirementJustification,
      assessmentPrerequisite,
      assessmentPrerequisiteText,
      assessmentPrerequisiteJustification,
      assessmentMethods,
      assessmentMethodsCount
    )
}

final class ExamLoadCSVPrinter(
    modules: Vector[Module],
    children: Vector[ModuleProtocol],
    po: String,
    assessmentMethods: Seq[AssessmentMethod]
) extends Logging {

  private val modulesToConsume = ListBuffer[UUID](modules.map(_.id)*)

  private def consume(module: UUID): Unit = {
    modulesToConsume -= module
  }

  private def instanceModulesOf(module: UUID): List[Module] =
    this.modules.filter(m => m.metadata.po.optional.exists(_.instanceOf == module)).toList

  private def printHeader(sb: StringBuilder) = {
    val header = List(
      "Semester",
      "Modul",
      "Modulnummer",
      "Teilmodule",
      "Modulart",
      "ECTS Teilmodul",
      "ECTS Gesamt",
      "Anwesenheitspflicht ja / nein",
      "Anwesenheitspflicht wenn ja, Mindestpräsenzzeit",
      "Anwesenheitspflicht wenn ja, Begründung",
      "Prüfungsvorleistung ja / nein",
      "Prüfungsvorleistung wenn ja, welche(s) (Teil)Modul(e)",
      "Prüfungsvorleistung wenn ja, Begründung",
      "Prüfungsformen / Gewichtung / Benotung",
      "Prüfungsleistungen pro (Teil)Modul",
    )
    // val header = List(
    //   "Semester",
    //   "Modul",
    //   "Modulnummer",
    //   "Teilmodule",
    //   "Modulart",
    //   "ECTS Teilmodul",
    //   "ECTS Gesamt",
    //   "AP",
    //   "AP Mindestpräsenzzeit",
    //   "AP Begründung",
    //   "PVL",
    //   "PVL Module",
    //   "PVL Begründung",
    //   "Prüfungsformen",
    //   "Prüfungsleistungen",
    // )
    sb.append(header.mkString(";"))
  }

  private def createRows(module: Module): List[Row] = {
    def moduleType(modulePO: ModulePOProtocol) = {
      val isMandatory = modulePO.mandatory.exists(p => p.po == po && p.specialization.isEmpty)
      if isMandatory then "PF" else "WF" // TODO: it also might be a WPF
    }

    def double(value: Double) = {
      val strValue             = value.toString
      val Array(int, decimals) = strValue.split('.')
      if decimals == "0" then int
      else s"$int,$decimals"
    }

    def attendanceRequirement(requirement: Option[AttendanceRequirement]) =
      requirement match {
        case Some(req) => ("ja", req.min.trim(), req.reason.trim())
        case None      => ("nein", "-", "-")
      }

    def assessmentPrerequisite(prerequisite: Option[AssessmentPrerequisite]) =
      prerequisite match {
        case Some(pre) => ("ja", pre.modules.trim(), pre.reason.trim())
        case None      => ("nein", "-", "-")
      }

    def assessmentMethods(methods: List[ModuleAssessmentMethodEntryProtocol]) =
      methods
        .map { m =>
          val methodLabel = this.assessmentMethods.find(_.id == m.method).fold(m.method)(_.deLabel)
          m.percentage match
            case Some(p) =>
              // assuming 0.0 denotes ungraded
              val percentageLabel = if p == 0.0 then "unbenotet" else s"${double(p)} %"
              s"${methodLabel} ($percentageLabel)"
            case None =>
              methodLabel
        }
        .mkString(" und ")

    def assessmentMethodsCount(methods: List[ModuleAssessmentMethodEntryProtocol]) =
      if methods.isEmpty then "" else methods.size.toString()

    def createDefaultRow(module: Module): Row = {
      consume(module.id)

      val (attReq, attReqText, attReqReason) = attendanceRequirement(module.metadata.attendanceRequirement)
      val (assPre, assPreText, assPreReason) = assessmentPrerequisite(module.metadata.assessmentPrerequisite)
      Row(
        semester = module.semester.mkString(","),
        module = module.metadata.title,
        moduleNumber = module.metadata.abbrev,
        submodule = "-",
        moduleType = moduleType(module.metadata.po),
        submoduleCredits = double(module.metadata.ects),
        totalCredits = double(module.metadata.ects),
        attendanceRequirement = attReq,
        attendanceRequirementText = attReqText,
        attendanceRequirementJustification = attReqReason,
        assessmentPrerequisite = assPre,
        assessmentPrerequisiteText = assPreText,
        assessmentPrerequisiteJustification = assPreReason,
        assessmentMethods = assessmentMethods(module.metadata.assessmentMethods.mandatory),
        assessmentMethodsCount = assessmentMethodsCount(module.metadata.assessmentMethods.mandatory),
      )
    }

    def createParentRow(module: Module): Row = {
      consume(module.id)

      val (attReq, attReqText, attReqReason) = attendanceRequirement(module.metadata.attendanceRequirement)
      val (assPre, assPreText, assPreReason) = assessmentPrerequisite(module.metadata.assessmentPrerequisite)
      Row(
        semester = module.semester.mkString(","),
        module = module.metadata.title,
        moduleNumber = module.metadata.abbrev,
        submodule = "",
        moduleType = moduleType(module.metadata.po),
        submoduleCredits = "",
        totalCredits = double(module.metadata.ects),
        attendanceRequirement = attReq,
        attendanceRequirementText = attReqText,
        attendanceRequirementJustification = attReqReason,
        assessmentPrerequisite = assPre,
        assessmentPrerequisiteText = assPreText,
        assessmentPrerequisiteJustification = assPreReason,
        assessmentMethods = assessmentMethods(module.metadata.assessmentMethods.mandatory),
        assessmentMethodsCount = assessmentMethodsCount(module.metadata.assessmentMethods.mandatory),
      )
    }

    def createChildRow(module: ModuleProtocol): Row = {
      consume(module.id.get)

      val (attReq, attReqText, attReqReason) = attendanceRequirement(module.metadata.attendanceRequirement)
      val (assPre, assPreText, assPreReason) = assessmentPrerequisite(module.metadata.assessmentPrerequisite)
      Row(
        semester = "",
        module = module.metadata.title,
        moduleNumber = module.metadata.abbrev,
        submodule = "",
        moduleType = moduleType(module.metadata.po),
        submoduleCredits = double(module.metadata.ects),
        totalCredits = "",
        attendanceRequirement = attReq,
        attendanceRequirementText = attReqText,
        attendanceRequirementJustification = attReqReason,
        assessmentPrerequisite = assPre,
        assessmentPrerequisiteText = assPreText,
        assessmentPrerequisiteJustification = assPreReason,
        assessmentMethods = assessmentMethods(module.metadata.assessmentMethods.mandatory),
        assessmentMethodsCount = assessmentMethodsCount(module.metadata.assessmentMethods.mandatory),
      )
    }

    val m = module.metadata

    if m.isGeneric then {
      val genericModule = createParentRow(module)
      val instances     = instanceModulesOf(module.id).map(createDefaultRow)
      genericModule :: instances
    } else {
      m.moduleRelation.match {
        case Some(ModuleRelationProtocol.Parent(childrenIds)) =>
          val parent = createParentRow(module)
          val children = childrenIds
            .map(id => this.children.find(_.id.get == id).get)
            .sortBy(_.metadata.title)
            .map(createChildRow)
          parent :: children.toList
        case Some(ModuleRelationProtocol.Child(_)) =>
          // child modules are rendered below their parent module
          Nil
        case None =>
          val isElectiveModule = module.metadata.po.optional.exists(_.po == po)
          if isElectiveModule then {
            // elective modules are rendered below their generic module
            Nil
          } else {
            List(createDefaultRow(module))
          }
      }
    }
  }

  private def assumeConsumption(): Unit = {
    if modulesToConsume.nonEmpty then {
      logger.error(s"non consumed modules: ${modulesToConsume.toList}")
    }
  }

  private def printModule(sb: StringBuilder, module: Module): Unit =
    for (row <- createRows(module)) do sb.append(s"\n${row.toList.mkString(";")}")

  def print(): String = {
    val sb = new StringBuilder()
    printHeader(sb)
    for (module <- modules) {
      printModule(sb, module)
    }
    assumeConsumption()
    sb.toString()
  }
}

final class ExamLoadService @Inject() (
    assessmentMethodService: AssessmentMethodService,
    gitCli: ModuleGitCLI,
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
