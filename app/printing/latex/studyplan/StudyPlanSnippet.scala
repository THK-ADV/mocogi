package printing.latex.studyplan

import java.util.UUID

import cats.data.NonEmptyList
import models.core.IDLabel
import models.MetadataProtocol
import models.ModulePOMandatoryProtocol
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.fmtDouble
import printing.latex.escape
import printing.latex.snippet.LatexContentSnippet
import service.artifact.modulecatalog.ModuleCatalogGenericModuleOccurrence
import service.artifact.modulecatalog.ModuleCatalogSemesterSelection
import service.artifact.modulecatalog.ModuleCatalogWarning
import service.artifact.modulecatalog.StudyPlanSection

final class StudyPlanSnippet(
    currentPO: String,
    modules: Vector[(UUID, MetadataProtocol)],
    sections: Option[NonEmptyList[StudyPlanSection]],
    semesterSelections: List[ModuleCatalogSemesterSelection],
    genericModuleOccurrences: List[ModuleCatalogGenericModuleOccurrence],
    specializations: List[IDLabel],
    warningCollector: ModuleCatalogWarning => Unit,
    isPreview: Boolean,
    messages: MessagesApi,
) extends LatexContentSnippet
    with Logging {

  private sealed trait StudyPlanTableHeadline

  private object StudyPlanTableHeadline {
    case object Base                               extends StudyPlanTableHeadline
    final case class Specialization(label: String) extends StudyPlanTableHeadline
  }

  private case class StudyPlanCandidate(
      id: UUID,
      metadata: MetadataProtocol,
      mandatoryPO: ModulePOMandatoryProtocol
  )

  private case class StudyPlanModule(
      id: UUID,
      title: String,
      hasPrecondition: Boolean,
      credits: Double,
      recommendedSemester: Int
  )

  private case class UnassignedStudyPlanModule(
      id: UUID,
      title: String,
      hasPrecondition: Boolean,
      credits: Double
  )

  private case class StudyPlanEntries(
      assigned: Vector[StudyPlanModule],
      unassigned: Vector[UnassignedStudyPlanModule],
      warnings: Vector[ModuleCatalogWarning]
  )

  private case class StudyPlanTable(
      headline: Option[StudyPlanTableHeadline],
      entries: Vector[StudyPlanModule],
      unassignedEntries: Vector[UnassignedStudyPlanModule],
      sections: Option[NonEmptyList[StudyPlanSection]],
      warnings: Vector[ModuleCatalogWarning]
  )

  private val selectedSemesters: Map[UUID, Int] =
    semesterSelections.map(s => s.moduleId -> s.selectedSemester).toMap

  private val occurrencesByModule: Map[UUID, List[ModuleCatalogGenericModuleOccurrence]] =
    genericModuleOccurrences.groupBy(_.moduleId)

  private val studyPlanTables: List[StudyPlanTable] = buildStudyPlanTables()

  studyPlanTables.flatMap(_.warnings).foreach(warningCollector)

  private def mandatoryPO(
      pos: List[ModulePOMandatoryProtocol],
      specialization: Option[String]
  ): Option[ModulePOMandatoryProtocol] = {
    val potentialPOs = pos.filter(p => p.po == currentPO && p.specialization == specialization)
    if potentialPOs.size == 1 then Some(potentialPOs.head) else None
  }

  private def hasPrecondition(m: MetadataProtocol): Boolean =
    m.assessmentPrerequisite.exists(_.modules.nonEmpty)

  private def warning(code: String, message: String, moduleId: UUID): ModuleCatalogWarning =
    ModuleCatalogWarning(code, message, Some(moduleId))

  private def selectedOrDefaultSemester(candidate: StudyPlanCandidate): (Option[Int], Vector[ModuleCatalogWarning]) = {
    val recommendedSemesters = candidate.mandatoryPO.recommendedSemester.distinct.sorted
    selectedSemesters.get(candidate.id) match {
      case Some(selected) =>
        Some(selected) -> Vector.empty
      case None if recommendedSemesters.nonEmpty =>
        val selected = recommendedSemesters.min
        val warnings =
          if recommendedSemesters.size > 1 then
            Vector(
              warning(
                "multiple_recommended_semesters",
                s"Module has multiple recommended semesters ${recommendedSemesters.mkString(", ")}; using $selected in the study plan.",
                candidate.id
              )
            )
          else Vector.empty
        Some(selected) -> warnings
      case None =>
        None -> Vector(
          warning(
            "missing_recommended_semester",
            "Mandatory module has no recommended semester and is not assigned in the study plan.",
            candidate.id
          )
        )
    }
  }

  private def genericDefaultWarning(candidate: StudyPlanCandidate): Option[ModuleCatalogWarning] =
    Option.when(candidate.metadata.isGeneric && !occurrencesByModule.contains(candidate.id))(
      warning(
        "generic_module_default_occurrence",
        "Generic module uses one default study-plan occurrence; configure genericModuleOccurrences to change this.",
        candidate.id
      )
    )

  private def studyPlanEntries(
      specialization: Option[String]
  ): StudyPlanEntries = {
    val candidates = modules
      .flatMap {
        case (id, m) =>
          // TODO: adapt to new implementation of parent modules
          val isNoChild = true // m.moduleRelation.isEmpty || m.moduleRelation.exists(_.isParent)
          mandatoryPO(m.po.mandatory, specialization).filter(_ => isNoChild).map { mandatoryPO =>
            StudyPlanCandidate(id, m, mandatoryPO)
          }
      }

    val empty = StudyPlanEntries(Vector.empty, Vector.empty, Vector.empty)

    candidates.foldLeft(empty) { (acc, candidate) =>
      val occurrenceOverrides =
        if candidate.metadata.isGeneric then occurrencesByModule.getOrElse(candidate.id, Nil)
        else Nil

      if occurrenceOverrides.nonEmpty then {
        val entries = occurrenceOverrides.toVector.flatMap { occurrence =>
          Vector.fill(occurrence.count)(
            StudyPlanModule(
              id = candidate.id,
              title = candidate.metadata.title,
              hasPrecondition = hasPrecondition(candidate.metadata),
              credits = candidate.metadata.ects,
              recommendedSemester = occurrence.semester
            )
          )
        }
        acc.copy(assigned = acc.assigned.appendedAll(entries))
      } else {
        val (semester, semesterWarnings) = selectedOrDefaultSemester(candidate)
        val warnings                     = semesterWarnings.appendedAll(genericDefaultWarning(candidate))
        semester match {
          case Some(recommendedSemester) =>
            acc.copy(
              assigned = acc.assigned.appended(
                StudyPlanModule(
                  id = candidate.id,
                  title = candidate.metadata.title,
                  hasPrecondition = hasPrecondition(candidate.metadata),
                  credits = candidate.metadata.ects,
                  recommendedSemester = recommendedSemester
                )
              ),
              warnings = acc.warnings.appendedAll(warnings)
            )
          case None =>
            acc.copy(
              unassigned = acc.unassigned.appended(
                UnassignedStudyPlanModule(
                  id = candidate.id,
                  title = candidate.metadata.title,
                  hasPrecondition = hasPrecondition(candidate.metadata),
                  credits = candidate.metadata.ects
                )
              ),
              warnings = acc.warnings.appendedAll(warnings)
            )
        }
      }
    } match {
      case StudyPlanEntries(assigned, unassigned, warnings) =>
        StudyPlanEntries(
          assigned.sortBy(m => (m.recommendedSemester, m.title)),
          unassigned.sortBy(_.title),
          warnings
        )
    }
  }

  private def buildTable(
      specialization: Option[String],
      headline: Option[StudyPlanTableHeadline],
      tableSections: Option[NonEmptyList[StudyPlanSection]]
  ): StudyPlanTable = {
    val entries = studyPlanEntries(specialization)
    StudyPlanTable(
      headline,
      entries.assigned,
      entries.unassigned,
      tableSections,
      entries.warnings
    )
  }

  private def buildStudyPlanTables(): List[StudyPlanTable] =
    if specializations.nonEmpty then
      buildTable(None, Some(StudyPlanTableHeadline.Base), None) :: specializations
        .sortBy(_.deLabel)
        .map { specialization =>
          buildTable(
            Some(specialization.id),
            Some(StudyPlanTableHeadline.Specialization(specialization.deLabel)),
            None
          )
        }
    else List(buildTable(None, None, sections))

  private def groupIntoSections(
      entries: Vector[StudyPlanModule],
      sections: NonEmptyList[StudyPlanSection]
  ): List[(StudyPlanSection, Vector[StudyPlanModule])] =
    sections
      .sortBy(_.untilSemester)
      .foldLeft((List.empty[(StudyPlanSection, Vector[StudyPlanModule])], 0)) {
        case ((acc, previousUntilSemester), section) =>
          val sectionEntries = entries.filter(entry =>
            previousUntilSemester < entry.recommendedSemester && entry.recommendedSemester <= section.untilSemester
          )
          val nextAcc =
            if sectionEntries.nonEmpty then acc.appended((section, sectionEntries))
            else acc
          (nextAcc, section.untilSemester)
      }
      ._1

  private def semesterRange(firstSemester: Int, lastSemester: Int): Range =
    firstSemester to lastSemester

  private def studyPlanColumnSpec(firstSemester: Int, lastSemester: Int): String = {
    val semesterCount = semesterRange(firstSemester, lastSemester).size
    val columnCount   = semesterCount + 3
    val pvWidth       = "0.04\\linewidth"
    val cpWidth       = "0.045\\linewidth"
    val semesterWidth = "0.055\\linewidth"
    val fixedWidths   = Seq(pvWidth, cpWidth)
      .appendedAll(List.fill(semesterCount)(semesterWidth))
      .map(width => s" - $width")
      .mkString
    val tableSpacing    = s" - ${2 * columnCount}\\tabcolsep - ${columnCount + 1}\\arrayrulewidth"
    val moduleWidth     = s"\\dimexpr\\linewidth$fixedWidths$tableSpacing\\relax"
    val moduleColumn    = s">{\\raggedright\\arraybackslash}p{$moduleWidth}"
    val pvColumn        = s">{\\centering\\arraybackslash}p{$pvWidth}"
    val cpColumn        = s">{\\centering\\arraybackslash}p{$cpWidth}"
    val semesterColumns = semesterRange(firstSemester, lastSemester)
      .map { semester =>
        val color = if semester % 2 == 1 then "\\columncolor{gray!12}" else ""
        s">{$color\\centering\\arraybackslash}p{$semesterWidth}"
      }
      .mkString("|")

    s"|$moduleColumn|$pvColumn|$cpColumn|$semesterColumns|"
  }

  private def tableHeader(firstSemester: Int, lastSemester: Int)(using lang: Lang): String = {
    val semesterCount   = semesterRange(firstSemester, lastSemester).size
    val semesterHeaders = semesterRange(firstSemester, lastSemester).map(_.toString).mkString(" & ")

    s"""\\hline
       |\\multicolumn{3}{|l|}{\\textbf{${messages("latex.module_catalog.study_plan.sections")}}} & \\multicolumn{$semesterCount}{c|}{\\textbf{${messages("latex.module_catalog.study_plan.semester_assignment")}}} \\\\
       |\\hline
       |\\textbf{${messages("latex.module_catalog.study_plan.column.module")}} & \\textbf{${messages("latex.module_catalog.study_plan.column.pv")}} & \\textbf{${messages("latex.module_catalog.study_plan.column.cp")}} & $semesterHeaders \\\\
       |\\hline
       |""".stripMargin
  }

  private def moduleLink(module: StudyPlanModule | UnassignedStudyPlanModule): String = {
    val (id, title) = module match {
      case StudyPlanModule(id, title, _, _, _)        => (id, title)
      case UnassignedStudyPlanModule(id, title, _, _) => (id, title)
    }
    s"\\hyperref[sec:${id.toString}]{${escape(title)}}"
  }

  private def printSectionRow(
      headline: String,
      entries: Vector[StudyPlanModule],
      firstSemester: Int,
      lastSemester: Int
  )(using builder: StringBuilder): Unit = {
    val emptySemesterCells = semesterRange(firstSemester, lastSemester).map(_ => "").mkString(" & ")
    builder.append(
      s"\\rowcolor{black}\\textcolor{white}{\\textbf{${escape(headline)}}} & & \\textcolor{white}{\\textbf{${fmtDouble(entries.map(_.credits).sum)}}} & $emptySemesterCells \\\\*\n"
    )
  }

  private def printModuleRow(module: StudyPlanModule, firstSemester: Int, lastSemester: Int)(
      using lang: Lang,
      builder: StringBuilder
  ): Unit = {
    val pv =
      if module.hasPrecondition then messages("latex.module_catalog.study_plan.pv.yes")
      else messages("latex.module_catalog.study_plan.pv.no")
    val semesterCredits = semesterRange(firstSemester, lastSemester)
      .map(semester => if semester == module.recommendedSemester then fmtDouble(module.credits) else "")
      .mkString(" & ")

    builder.append(s"${moduleLink(module)} & $pv & ${fmtDouble(module.credits)} & $semesterCredits \\\\\n")
  }

  private def printFooterRow(entries: Vector[StudyPlanModule], firstSemester: Int, lastSemester: Int)(
      using lang: Lang,
      builder: StringBuilder
  ): Unit = {
    val semesterTotals = semesterRange(firstSemester, lastSemester)
      .map { semester =>
        val sum = entries.filter(_.recommendedSemester == semester).map(_.credits).sum
        if sum > 0 then fmtDouble(sum) else ""
      }
      .mkString(" & ")

    builder.append(
      s"\\hline\n\\rowcolor{gray!20}\\textbf{${messages("latex.module_catalog.study_plan.footer.total")}} & & \\textbf{${fmtDouble(entries.map(_.credits).sum)}} & $semesterTotals \\\\\n"
    )
  }

  private def unassignedColumnSpec: String = {
    val columnCount  = 3
    val pvWidth      = "0.04\\linewidth"
    val cpWidth      = "0.045\\linewidth"
    val tableSpacing = s" - ${2 * columnCount}\\tabcolsep - ${columnCount + 1}\\arrayrulewidth"
    val moduleWidth  = s"\\dimexpr\\linewidth - $pvWidth - $cpWidth$tableSpacing\\relax"
    val moduleColumn = s">{\\raggedright\\arraybackslash}p{$moduleWidth}"
    val pvColumn     = s">{\\centering\\arraybackslash}p{$pvWidth}"
    val cpColumn     = s">{\\centering\\arraybackslash}p{$cpWidth}"

    s"|$moduleColumn|$pvColumn|$cpColumn|"
  }

  private def printUnassignedModuleRow(
      module: UnassignedStudyPlanModule
  )(using lang: Lang, builder: StringBuilder): Unit = {
    val pv =
      if module.hasPrecondition then messages("latex.module_catalog.study_plan.pv.yes")
      else messages("latex.module_catalog.study_plan.pv.no")

    builder.append(s"${moduleLink(module)} & $pv & ${fmtDouble(module.credits)} \\\\\n")
  }

  private def printUnassignedModules(
      modules: Vector[UnassignedStudyPlanModule]
  )(using lang: Lang, builder: StringBuilder): Unit = {
    builder.append(
      s"""\\vspace{1em}
         |\\begin{tabular}{${unassignedColumnSpec}}
         |\\hline
         |\\rowcolor{black}\\textcolor{white}{\\textbf{${messages("latex.module_catalog.study_plan.unassigned")}}} & & \\textcolor{white}{\\textbf{${fmtDouble(modules.map(_.credits).sum)}}} \\\\*
         |\\hline
         |\\textbf{${messages("latex.module_catalog.study_plan.column.module")}} & \\textbf{${messages("latex.module_catalog.study_plan.column.pv")}} & \\textbf{${messages("latex.module_catalog.study_plan.column.cp")}} \\\\
         |\\hline
         |""".stripMargin
    )
    modules.foreach(printUnassignedModuleRow)
    builder.append(
      s"""\\hline
         |\\end{tabular}
         |\\vspace{1em}
         |\\textit{${messages("latex.module_catalog.study_plan.unassigned.explanation")}}
         |""".stripMargin
    )
  }

  private def logUnassignedModules(modules: Vector[UnassignedStudyPlanModule]): Unit = {
    val moduleList = modules.map(module => s"${module.title} (${module.id})").mkString(", ")
    logger.error(
      s"mandatory modules without recommended semester in PO $currentPO are omitted from study plan: $moduleList"
    )
  }

  private def headlineText(headline: StudyPlanTableHeadline)(using lang: Lang): String =
    headline match {
      case StudyPlanTableHeadline.Base =>
        messages("latex.module_catalog.study_plan.base")
      case StudyPlanTableHeadline.Specialization(label) =>
        messages("latex.module_catalog.study_plan.specialization", label)
    }

  private def printStudyPlan(
      table: StudyPlanTable,
      firstSemester: Int,
      lastSemester: Int
  )(using lang: Lang, builder: StringBuilder): Unit = {
    table.headline.foreach(headline => builder.append(s"\\subsection*{${escape(headlineText(headline))}}\n"))

    val columns             = studyPlanColumnSpec(firstSemester, lastSemester)
    val columnCount         = semesterRange(firstSemester, lastSemester).size + 3
    val header              = tableHeader(firstSemester, lastSemester)
    val continuationMessage = messages("latex.module_catalog.study_plan.header.continuation")

    builder.append(
      s"""\\begingroup
         |\\small
         |\\setlength{\\tabcolsep}{3pt}
         |\\setlength{\\extrarowheight}{1pt}
         |\\rowcolors{3}{white}{gray!6}
         |\\begin{tabularx}{\\linewidth}{$columns}
         |$header
         |\\endfirsthead
         |\\multicolumn{$columnCount}{l}{\\textit{$continuationMessage}} \\\\
         |$header
         |\\endhead
         |\\hline
         |\\endfoot
         |\\hline
         |\\endlastfoot
         |""".stripMargin
    )

    table.sections match {
      case Some(sections) =>
        val groupedEntries = groupIntoSections(table.entries, sections)
        groupedEntries.foreach {
          case (section, entries) =>
            printSectionRow(section.headline, entries, firstSemester, lastSemester)
            entries.foreach(printModuleRow(_, firstSemester, lastSemester))
        }
        val groupedEntryRows      = groupedEntries.flatMap(_._2)
        val entriesWithoutSection = table.entries.diff(groupedEntryRows)
        if entriesWithoutSection.nonEmpty then {
          printSectionRow(
            messages("latex.module_catalog.study_plan.section.unassigned"),
            entriesWithoutSection,
            firstSemester,
            lastSemester
          )
          entriesWithoutSection.foreach(printModuleRow(_, firstSemester, lastSemester))
        }
      case None =>
        var currentSemester = firstSemester
        for (entry <- table.entries) {
          if entry.recommendedSemester > currentSemester then {
            builder.append("\\hline\n")
            currentSemester = entry.recommendedSemester
          }
          printModuleRow(entry, firstSemester, lastSemester)
        }
    }

    printFooterRow(table.entries, firstSemester, lastSemester)

    builder.append(
      """\end{tabularx}
        |\endgroup
        |""".stripMargin
    )
  }

  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    val nonEmptyTables = studyPlanTables.filter(table => table.entries.nonEmpty || table.unassignedEntries.nonEmpty)

    if nonEmptyTables.nonEmpty then {
      builder.append(s"\\section{${messages("latex.module_catalog.study_plan.headline")}}\n")

      nonEmptyTables.foreach { table =>
        val firstSemester = table.entries.minByOption(_.recommendedSemester).map(_.recommendedSemester)
        val lastSemester  = table.entries.maxByOption(_.recommendedSemester).map(_.recommendedSemester)

        if firstSemester.isDefined && lastSemester.isDefined then {
          printStudyPlan(table, firstSemester.get, lastSemester.get)
        } else {
          table.headline.foreach(headline => builder.append(s"\\subsection*{${escape(headlineText(headline))}}\n"))
        }

        if table.unassignedEntries.nonEmpty then {
          if isPreview then printUnassignedModules(table.unassignedEntries)
          else logUnassignedModules(table.unassignedEntries)
        }
      }

      finish
    }
  }
}
