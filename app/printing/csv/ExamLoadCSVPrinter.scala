package printing.csv

import java.util.UUID

import models.*
import models.core.AssessmentMethod
import play.api.Logging

/**
 *  This feature is currently experimental. Feedback is expected for further development.
 */
final class ExamLoadCSVPrinter(
    modules: Vector[ExamLoadModule],
    childrenById: Map[UUID, ModuleProtocol],
    assessmentMethods: Map[UUID, Seq[AssessmentMethod]],
    electiveGroups: Vector[ElectiveGroup]
) extends Logging {

  private val writtenExamAssessmentMethodIds =
    Set("e-exam", "written-exam", "written-exam-answer-choice-method")

  private val header = List(
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

  private def escapeCell(value: String) =
    if value.exists(c => c == ';' || c == '"' || c == '\n' || c == '\r') then s""""${value.replace("\"", "\"\"")}""""
    else value

  private def printHeader(sb: StringBuilder) =
    sb.append(header.mkString(";"))

  private def createRows(module: ExamLoadModule, isElective: Boolean): List[Row] = {
    def moduleTypeLabel(p: MetadataProtocol) = if isElective || p.isGeneric then "WPF" else "PF"

    def ectsLabel(value: Double) =
      if value.isWhole then value.toInt.toString
      else value.toString.replace('.', ',')

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

    def assessmentMethodLabels(id: UUID) =
      assessmentMethods
        .getOrElse(id, Nil)
        .map(method => if writtenExamAssessmentMethodIds.contains(method.id) then "Klausurarbeit" else method.deLabel)
        .distinct
        .sorted

    def assessmentMethodsLabel(id: UUID) = {
      val labels = assessmentMethodLabels(id)
      if labels.isEmpty then "-" else labels.mkString(" und/oder ")
    }

    def assessmentMethodsCountLabel(id: UUID) =
      assessmentMethodLabels(id).size.toString

    def createRow(id: UUID, module: MetadataProtocol, semesterLabel: String): Row = {
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

    val semesterLabel = module.semesters.mkString(",")

    module.metadata.moduleRelation match {
      case Some(relation) =>
        val parent = createRow(module.id, module.metadata, semesterLabel)
          .copy(submodule = "", submoduleCredits = "")

        val children = relation.children.toList
          .flatMap { id =>
            childrenById.get(id).orElse {
              logger.error(s"Unable to render missing or inactive child module $id of parent ${module.id}")
              None
            }
          }
          .sortBy(_.metadata.title)
          .map(child => createRow(child.id.get, child.metadata, semesterLabel = "").copy(totalCredits = ""))
        parent :: children
      case None =>
        List(createRow(module.id, module.metadata, semesterLabel))
    }
  }

  private def printRows(sb: StringBuilder, rows: List[Row]): Unit =
    for (row <- rows) do sb.append(s"\n${row.toList.map(escapeCell).mkString(";")}")

  private def printModule(sb: StringBuilder, module: ExamLoadModule, isElective: Boolean = false): Unit =
    printRows(sb, createRows(module, isElective))

  private def printGenericDelimiter(sb: StringBuilder, title: String): Unit =
    sb.append(s"\n${List.fill(header.size)("k. A.").updated(1, title).map(escapeCell).mkString(";")}")

  def print(): String = {
    val sb = new StringBuilder()
    printHeader(sb)
    for (module <- modules) {
      printModule(sb, module)
    }
    for (group <- electiveGroups) {
      printGenericDelimiter(sb, group.genericTitle)
      for (module <- group.modules) {
        printModule(sb, module, isElective = true)
      }
    }
    sb.toString()
  }
}
