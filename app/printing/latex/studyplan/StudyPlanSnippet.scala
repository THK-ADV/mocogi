package printing.latex.studyplan

import java.util.UUID

import models.MetadataProtocol
import models.ModulePOMandatoryProtocol
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.fmtDouble
import printing.latex.escape
import cats.data.NonEmptyList
import printing.latex.snippet.LatexContentSnippet

final class StudyPlanSnippet(
    currentPO: String,
    modules: Vector[(UUID, MetadataProtocol)],
    sections: Option[NonEmptyList[StudyPlanSection]],
    isPreview: Boolean,
    messages: MessagesApi,
) extends LatexContentSnippet
    with Logging {

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

  // Don't support specializations for now
  private def singleNonSpecializedMandatoryPO(
      pos: List[ModulePOMandatoryProtocol]
  ): Option[ModulePOMandatoryProtocol] = {
    val potentialPOs = pos.filter(p => p.po == currentPO && p.specialization.isEmpty)
    if potentialPOs.size == 1 then Some(potentialPOs.head) else None
  }

  // Only support the earliest recommended semester for now.
  // TODO: The user should pick how multiple semesters are resolved
  // TODO: The user should also decide if one module occurs multiple times (e.g., generic elective modules)
  private def recommendedSemester(po: ModulePOMandatoryProtocol): Int =
    po.recommendedSemester.min

  // Assume assessment prerequisites are the only preconditions for now
  private def hasPrecondition(m: MetadataProtocol): Boolean =
    m.assessmentPrerequisite.exists(_.modules.nonEmpty)

  private def studyPlanEntries(
      modules: Vector[(UUID, MetadataProtocol)]
  ): (Vector[StudyPlanModule], Vector[UnassignedStudyPlanModule]) = {
    val candidates = modules
      .flatMap {
        case (id, m) =>
          val isNoChild = m.moduleRelation.isEmpty || m.moduleRelation.exists(_.isParent)
          singleNonSpecializedMandatoryPO(m.po.mandatory).filter(_ => isNoChild).map { mandatoryPO =>
            StudyPlanCandidate(id, m, mandatoryPO)
          }
      }

    val (studyPlanModules, unassignedStudyPlanModules) = candidates.partitionMap(candidate =>
      if candidate.mandatoryPO.recommendedSemester.nonEmpty then
        Left(
          StudyPlanModule(
            id = candidate.id,
            title = candidate.metadata.title,
            hasPrecondition = hasPrecondition(candidate.metadata),
            credits = candidate.metadata.ects,
            recommendedSemester = recommendedSemester(candidate.mandatoryPO)
          )
        )
      else
        Right(
          UnassignedStudyPlanModule(
            id = candidate.id,
            title = candidate.metadata.title,
            hasPrecondition = hasPrecondition(candidate.metadata),
            credits = candidate.metadata.ects
          )
        )
    )

    (studyPlanModules.sortBy(m => (m.recommendedSemester, m.title)), unassignedStudyPlanModules.sortBy(_.title))
  }

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

  private def printStudyPlan(
      studyPlanEntries: Vector[StudyPlanModule],
      firstSemester: Int,
      lastSemester: Int
  )(using lang: Lang, builder: StringBuilder): Unit = {
    builder.append(s"\\section{${messages("latex.module_catalog.study_plan.headline")}}\n")

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

    sections match {
      case Some(sections) =>
        val groupedEntries = groupIntoSections(studyPlanEntries, sections)
        groupedEntries.foreach {
          case (section, entries) =>
            printSectionRow(section.headline, entries, firstSemester, lastSemester)
            entries.foreach(printModuleRow(_, firstSemester, lastSemester))
        }
        val groupedModuleIds = groupedEntries.flatMap(_._2.map(_.id)).toSet
        val entriesWithoutSection = studyPlanEntries.filterNot(entry => groupedModuleIds.contains(entry.id))
        if entriesWithoutSection.nonEmpty then {
          printSectionRow(messages("latex.module_catalog.study_plan.section.unassigned"), entriesWithoutSection, firstSemester, lastSemester)
          entriesWithoutSection.foreach(printModuleRow(_, firstSemester, lastSemester))
        }
      case None =>
        var currentSemester = firstSemester
        for (entry <- studyPlanEntries) {
          if entry.recommendedSemester > currentSemester then {
            builder.append("\\hline\n")
            currentSemester = entry.recommendedSemester
          }
          printModuleRow(entry, firstSemester, lastSemester)
        }
    }

    printFooterRow(studyPlanEntries, firstSemester, lastSemester)

    builder.append(
      """\end{tabularx}
        |\endgroup
        |""".stripMargin
    )
  }

  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    val (entries, unassignedEntries) = studyPlanEntries(modules)
    val firstSemester                = entries.minByOption(_.recommendedSemester).map(_.recommendedSemester)
    val lastSemester                 = entries.maxByOption(_.recommendedSemester).map(_.recommendedSemester)

    if firstSemester.isDefined && lastSemester.isDefined then {
      printStudyPlan(entries, firstSemester.get, lastSemester.get)
      if unassignedEntries.nonEmpty then {
        if isPreview then printUnassignedModules(unassignedEntries)
        else logUnassignedModules(unassignedEntries)
      }

      finish
    }
  }
}
