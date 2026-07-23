package printing.csv

import java.util.UUID

import scala.collection.mutable.ListBuffer

import models.*
import models.core.AssessmentMethod
import play.api.Logging

/**
 *  This feature is currently experimental. Feedback is expected for further development.
 */
final class ExamLoadCSVPrinter(
    modules: Vector[MandatoryModule],
    children: Vector[ModuleProtocol],
    assessmentMethods: Map[UUID, Seq[AssessmentMethod]]
) extends Logging {

  private val writtenExamAssessmentMethodIds =
    Set("e-exam", "written-exam", "written-exam-answer-choice-method")

  private val modulesToConsume = ListBuffer[UUID](modules.map(_.id)*)

  private def consume(module: UUID): Unit = {
    modulesToConsume -= module
  }

  private def escapeCell(value: String) =
    if value.exists(c => c == ';' || c == '"' || c == '\n' || c == '\r') then s""""${value.replace("\"", "\"\"")}""""
    else value

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
    sb.append(header.map(escapeCell).mkString(";"))
  }

  private def createRows(module: MandatoryModule): List[Row] = {
    def moduleTypeLabel(p: MetadataProtocol) = if !p.isGeneric then "PF" else "WPF"

    def ectsLabel(value: Double) = {
      val strValue             = value.toString
      val Array(int, decimals) = strValue.split('.')
      if decimals == "0" then int
      else s"$int,$decimals"
    }

    def attendanceRequirementLabel(v: Option[AttendanceRequirement]) =
      v match {
        case Some(req) => ("ja", req.min.trim(), req.reason.trim())
        case None      => ("nein", "-", "-")
      }

    def assessmentPrerequisiteLabel(v: Option[AssessmentPrerequisite]) =
      v match {
        case Some(pre) => ("ja", pre.modules.trim(), pre.reason.trim())
        case None      => ("nein", "-", "-")
      }

    def assessmentMethodsLabel(id: UUID) =
      assessmentMethods.get(id).filter(_.nonEmpty) match {
        case Some(methods) =>
          methods
            .map { method =>
              if writtenExamAssessmentMethodIds.contains(method.id) then "Klausurarbeit"
              else method.deLabel
            }
            .distinct
            .sorted
            .mkString(" und/oder ")
        case None => "-"
      }

    def assessmentMethodsCountLabel(id: UUID) =
      assessmentMethods.get(id).fold(0)(_.size).toString

    def createRow(id: UUID, module: MetadataProtocol, semesterLabel: String): Row = {
      consume(id)

      val (attReq, attReqText, attReqReason) = attendanceRequirementLabel(module.attendanceRequirement)
      val (assPre, assPreText, assPreReason) = assessmentPrerequisiteLabel(module.assessmentPrerequisite)

      Row(
        semester = semesterLabel,
        module = module.title,
        moduleNumber = module.abbrev,
        submodule = "-",
        moduleType = moduleTypeLabel(module),
        submoduleCredits = ectsLabel(module.ects),
        totalCredits = ectsLabel(module.ects),
        attendanceRequirement = attReq,
        attendanceRequirementText = attReqText,
        attendanceRequirementJustification = attReqReason,
        assessmentPrerequisite = assPre,
        assessmentPrerequisiteText = assPreText,
        assessmentPrerequisiteJustification = assPreReason,
        assessmentMethods = assessmentMethodsLabel(id),
        assessmentMethodsCount = assessmentMethodsCountLabel(id),
      )
    }

    module.metadata.moduleRelation.match {
      case Some(ModuleRelationProtocol.Parent(childrenIds)) =>
        val parent = createRow(module.id, module.metadata, module.semesters.mkString(","))
          .copy(submodule = "", submoduleCredits = "")

        val children = childrenIds
          .map(id => this.children.find(_.id.get == id).get)
          .sortBy(_.metadata.title)
          .map(module => createRow(module.id.get, module.metadata, "").copy(semester = "", totalCredits = ""))
        parent :: children.toList
      case Some(ModuleRelationProtocol.Child(_)) =>
        // child modules are rendered below their parent module
        Nil
      case None =>
        List(createRow(module.id, module.metadata, module.semesters.mkString(",")))
    }
  }

  private def assumeConsumption(): Unit = {
    if modulesToConsume.nonEmpty then {
      logger.error(s"non consumed modules: ${modulesToConsume.toList}")
    }
  }

  private def printModule(sb: StringBuilder, module: MandatoryModule): Unit =
    for (row <- createRows(module)) do sb.append(s"\n${row.toList.map(escapeCell).mkString(";")}")

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
