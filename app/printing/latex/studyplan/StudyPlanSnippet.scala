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
import service.artifact.modulecatalog.ModuleCatalogModuleDistribution
import service.artifact.modulecatalog.ModuleCatalogSemesterSelection
import service.artifact.modulecatalog.ModuleCatalogWarning
import service.artifact.modulecatalog.StudyPlanSection

final class StudyPlanSnippet(
    currentPO: String,
    // child modules are excluded by the caller, otherwise their credits would count twice
    modules: Vector[(UUID, MetadataProtocol)],
    sections: Option[NonEmptyList[StudyPlanSection]],
    semesterSelections: List[ModuleCatalogSemesterSelection],
    genericModuleOccurrences: List[ModuleCatalogGenericModuleOccurrence],
    alternativeGenericModuleOccurrences: List[ModuleCatalogGenericModuleOccurrence],
    specializations: List[IDLabel],
    isPreview: Boolean,
    messages: MessagesApi,
    alternativeModuleDistributions: List[ModuleCatalogModuleDistribution] = Nil,
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

  private case class StudyPlanTable(
      specialization: Option[IDLabel],
      entries: Vector[StudyPlanModule],
      unassignedEntries: Vector[UnassignedStudyPlanModule],
      sections: Option[NonEmptyList[StudyPlanSection]],
      warnings: Vector[ModuleCatalogWarning]
  )

  private case class StudyPlanContext(
      partTime: Boolean,
      headlineKey: String,
      continuationKey: String,
      sections: Option[NonEmptyList[StudyPlanSection]],
      selectedSemesters: Map[UUID, Int],
      occurrencesByModule: Map[UUID, List[ModuleCatalogGenericModuleOccurrence]],
      distributionsByModule: Map[UUID, List[Int]]
  )

  private val defaultContext = StudyPlanContext(
    partTime = false,
    headlineKey = "latex.module_catalog.study_plan.headline",
    continuationKey = "latex.module_catalog.study_plan.header.continuation",
    sections = sections,
    selectedSemesters = semesterSelections.map(s => s.moduleId -> s.selectedSemester).toMap,
    occurrencesByModule = genericModuleOccurrences.groupBy(_.moduleId),
    distributionsByModule = Map.empty
  )

  private val alternativeContext = StudyPlanContext(
    partTime = true,
    headlineKey = "latex.module_catalog.study_plan.alternative.headline",
    continuationKey = "latex.module_catalog.study_plan.alternative.header.continuation",
    sections = None,
    selectedSemesters = Map.empty,
    occurrencesByModule = alternativeGenericModuleOccurrences.groupBy(_.moduleId),
    distributionsByModule = alternativeModuleDistributions.map(d => d.moduleId -> d.semesters).toMap
  )

  private val defaultTables     = tablesOf(defaultContext)
  private val alternativeTables = tablesOf(alternativeContext)

  val warnings: List[ModuleCatalogWarning] =
    (defaultTables ++ alternativeTables).flatMap(_.warnings)

  private def tablesOf(context: StudyPlanContext): List[StudyPlanTable] =
    if specializations.nonEmpty then
      studyPlanTable(context, None, None) :: specializations
        .sortBy(_.deLabel)
        .map(specialization => studyPlanTable(context, Some(specialization), None))
    else List(studyPlanTable(context, None, context.sections))

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

  private def selectedOrDefaultSemester(
      context: StudyPlanContext,
      candidate: StudyPlanCandidate
  ): (Option[Int], Vector[ModuleCatalogWarning]) =
    if context.partTime then
      candidate.mandatoryPO.recommendedSemesterPartTime match {
        case Some(semester) =>
          Some(semester) -> Vector.empty
        case None =>
          None -> Vector(
            warning(
              "missing_recommended_semester_part_time",
              "Mandatory module has no part-time recommended semester and is not assigned in the alternative study plan.",
              candidate.id
            )
          )
      }
    else {
      val recommendedSemesters = candidate.mandatoryPO.recommendedSemester.distinct.sorted
      context.selectedSemesters.get(candidate.id) match {
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

  private def genericDefaultWarning(
      context: StudyPlanContext,
      candidate: StudyPlanCandidate
  ): Option[ModuleCatalogWarning] =
    Option.when(candidate.metadata.isGeneric && !context.occurrencesByModule.contains(candidate.id))(
      warning(
        "generic_module_default_occurrence",
        "Generic module uses one default study-plan occurrence; configure genericModuleOccurrences to change this.",
        candidate.id
      )
    )

  private def studyPlanTable(
      context: StudyPlanContext,
      specialization: Option[IDLabel],
      tableSections: Option[NonEmptyList[StudyPlanSection]]
  ): StudyPlanTable = {
    val candidates = modules.flatMap {
      case (id, m) =>
        mandatoryPO(m.po.mandatory, specialization.map(_.id)).map(StudyPlanCandidate(id, m, _))
    }
    val (rows, rowWarnings)    = candidates.map(rowsOf(context, _)).unzip
    val (unassigned, assigned) = rows.flatten.partitionMap(identity)

    StudyPlanTable(
      specialization,
      assigned.sortBy(m => (m.recommendedSemester, m.title)),
      unassigned.sortBy(_.title),
      tableSections,
      rowWarnings.flatten
    )
  }

  /** One row per planned occurrence of the module, or a single unassigned row if no semester applies. */
  private def distributedCredits(total: Double, count: Int): Vector[Double] = {
    val cents              = (BigDecimal(total).setScale(2, BigDecimal.RoundingMode.HALF_UP) * 100).toInt
    val (share, remainder) = cents / count -> cents % count
    Vector.tabulate(count)(i => (BigDecimal(share + (if i < remainder then 1 else 0)) / 100).toDouble)
  }

  private def fmtCredits(credits: Double): String =
    fmtDouble(BigDecimal(credits).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)

  private def rowsOf(
      context: StudyPlanContext,
      candidate: StudyPlanCandidate
  ): (Vector[Either[UnassignedStudyPlanModule, StudyPlanModule]], Vector[ModuleCatalogWarning]) = {
    val metadata                                            = candidate.metadata
    val occurrences                                         = if metadata.isGeneric then context.occurrencesByModule.getOrElse(candidate.id, Nil) else Nil
    def row(semester: Int, credits: Double = metadata.ects) =
      Right(StudyPlanModule(candidate.id, metadata.title, hasPrecondition(metadata), credits, semester))

    if occurrences.nonEmpty then
      occurrences.toVector.flatMap(occurrence => Vector.fill(occurrence.count)(row(occurrence.semester))) ->
        Vector.empty
    else if context.distributionsByModule.contains(candidate.id) then {
      val semesters = context.distributionsByModule(candidate.id)
      semesters.toVector.zip(distributedCredits(metadata.ects, semesters.size)).map(row) -> Vector.empty
    } else {
      val (semester, warnings) = selectedOrDefaultSemester(context, candidate)
      val unassigned           =
        Left(UnassignedStudyPlanModule(candidate.id, metadata.title, hasPrecondition(metadata), metadata.ects))
      Vector(semester.fold(unassigned)(row(_))) ->
        warnings.appendedAll(semester.flatMap(_ => genericDefaultWarning(context, candidate)))
    }
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

  private def studyPlanColumnSpec(firstSemester: Int, lastSemester: Int, partTime: Boolean): String = {
    val semesterCount = semesterRange(firstSemester, lastSemester).size
    val columnCount   = semesterCount + 3
    val pvWidth       = "0.04\\linewidth"
    val cpWidth       = "0.045\\linewidth"
    val semesterWidth = if partTime then "0.04\\linewidth" else "0.055\\linewidth"
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
      s"\\rowcolor{black}\\textcolor{white}{\\textbf{${escape(headline)}}} & & \\textcolor{white}{\\textbf{${fmtCredits(entries.map(_.credits).sum)}}} & $emptySemesterCells \\\\*\n"
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
      .map(semester => if semester == module.recommendedSemester then fmtCredits(module.credits) else "")
      .mkString(" & ")

    builder.append(s"${moduleLink(module)} & $pv & ${fmtCredits(module.credits)} & $semesterCredits \\\\\n")
  }

  private def printFooterRow(entries: Vector[StudyPlanModule], firstSemester: Int, lastSemester: Int)(
      using lang: Lang,
      builder: StringBuilder
  ): Unit = {
    val semesterTotals = semesterRange(firstSemester, lastSemester)
      .map { semester =>
        val sum = entries.filter(_.recommendedSemester == semester).map(_.credits).sum
        if sum > 0 then fmtCredits(sum) else ""
      }
      .mkString(" & ")

    builder.append(
      s"\\hline\n\\rowcolor{gray!20}\\textbf{${messages("latex.module_catalog.study_plan.footer.total")}} & & \\textbf{${fmtCredits(entries.map(_.credits).sum)}} & $semesterTotals \\\\\n"
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

    builder.append(s"${moduleLink(module)} & $pv & ${fmtCredits(module.credits)} \\\\\n")
  }

  private def printUnassignedModules(
      modules: Vector[UnassignedStudyPlanModule]
  )(using lang: Lang, builder: StringBuilder): Unit = {
    builder.append(
      s"""\\vspace{1em}
         |\\begin{tabular}{${unassignedColumnSpec}}
         |\\hline
         |\\rowcolor{black}\\textcolor{white}{\\textbf{${messages("latex.module_catalog.study_plan.unassigned")}}} & & \\textcolor{white}{\\textbf{${fmtCredits(modules.map(_.credits).sum)}}} \\\\*
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

  private def logUnassignedModules(modules: Vector[UnassignedStudyPlanModule], partTime: Boolean): Unit = {
    val moduleList = modules.map(module => s"${module.title} (${module.id})").mkString(", ")
    logger.error(
      s"mandatory modules without recommended semester${if partTime then " (part-time)" else ""} in PO $currentPO are omitted from study plan: $moduleList"
    )
  }

  /** Tables are only labelled when the PO has specializations and therefore shows more than one table. */
  private def headline(table: StudyPlanTable)(using lang: Lang): Option[String] =
    Option.when(specializations.nonEmpty)(
      table.specialization.fold(messages("latex.module_catalog.study_plan.base"))(specialization =>
        messages("latex.module_catalog.study_plan.specialization", specialization.deLabel)
      )
    )

  private def printStudyPlan(
      context: StudyPlanContext,
      table: StudyPlanTable,
      firstSemester: Int,
      lastSemester: Int
  )(using lang: Lang, builder: StringBuilder): Unit = {
    headline(table).foreach(text => builder.append(s"\\subsection*{${escape(text)}}\n"))

    val columns             = studyPlanColumnSpec(firstSemester, lastSemester, context.partTime)
    val columnCount         = semesterRange(firstSemester, lastSemester).size + 3
    val header              = tableHeader(firstSemester, lastSemester)
    val continuationMessage = messages(context.continuationKey)

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

  private def printStudyPlans(
      context: StudyPlanContext,
      tables: List[StudyPlanTable]
  )(using lang: Lang, builder: StringBuilder): Unit = {
    val nonEmptyTables = tables.filter(table => table.entries.nonEmpty || table.unassignedEntries.nonEmpty)

    if nonEmptyTables.nonEmpty then {
      builder.append(s"\\section{${messages(context.headlineKey)}}\n")

      nonEmptyTables.foreach { table =>
        val firstSemester = table.entries.minByOption(_.recommendedSemester).map(_.recommendedSemester)
        val lastSemester  = table.entries.maxByOption(_.recommendedSemester).map(_.recommendedSemester)

        if firstSemester.isDefined && lastSemester.isDefined then {
          printStudyPlan(context, table, firstSemester.get, lastSemester.get)
        } else {
          headline(table).foreach(text => builder.append(s"\\subsection*{${escape(text)}}\n"))
        }

        if table.unassignedEntries.nonEmpty then {
          if isPreview then printUnassignedModules(table.unassignedEntries)
          else logUnassignedModules(table.unassignedEntries, context.partTime)
        }
      }
    }
  }

  override def print(using lang: Lang, builder: StringBuilder): Unit = {
    printStudyPlans(defaultContext, defaultTables)
    if alternativeTables.exists(t => t.entries.nonEmpty || t.unassignedEntries.nonEmpty) then {
      builder.append("\\clearpage\n\\begin{landscape}\n")
      printStudyPlans(alternativeContext, alternativeTables)
      builder.append("\\end{landscape}\n")
    }
    if (defaultTables ++ alternativeTables).exists(t => t.entries.nonEmpty || t.unassignedEntries.nonEmpty) then finish
  }
}
