package printing.latex

import java.time.format.DateTimeFormatter
import java.time.LocalDate

import scala.collection.mutable

import models.*
import models.core.ExamPhases.ExamPhase
import models.core.Identity
import service.artifact.examlist.*
import ops.appendOpt
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import printing.fmtDouble
import printing.LocalizedStrings

/** Renders prepared exam list entries. Named exams get their own indented rows below the module. */
final class ExamListsLatexPrinter(
    entries: List[PreparedExamListEntry],
    data: ExamListData,
    semester: Semester,
    date: Option[LocalDate],
    isPreview: Boolean,
    messages: MessagesApi
)(using lang: Lang) {

  // (ID, Label, Abbreviation)
  opaque type POShort = (String, String, String)

  private val builder: StringBuilder = new StringBuilder()

  private val strings = new LocalizedStrings(messages)(using lang)

  private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  private val assessmentMethodsById = data.assessmentMethods.map(m => m.id -> m).toMap

  private val peopleById = data.identities.map(p => p.id -> p).toMap

  private val genericModulesById = data.genericModules.map(m => m.id -> m).toMap

  private val studyProgram = data.studyProgram

  private val rowWidth = "Lp{.025\\linewidth}"

  private val moduleTitleWidthValue = .14

  private val moduleTitleWidth = s"Lp{$moduleTitleWidthValue\\linewidth}"

  private val moduleExamWidth = "Lp{.24\\linewidth}"

  private val assessmentTitleWidth = "Lp{.17\\linewidth}"

  private val examinerWidth = "Lp{.13\\linewidth}"

  private val examPhasesWidth = "Lp{.12\\linewidth}"

  private val registrationPeriodWidth = "Lp{.14\\linewidth}"

  private val genericModuleLegend = mutable.LinkedHashSet[ModuleCore]()

  private var usedP = false

  private var usedW = false

  def print(): StringBuilder = {
    builder.append("\\documentclass[12pt, oneside]{article}\n")
    packages()
    commands()
    builder.append(s"""\\begin{document}
                      |\\selectlanguage{${strings.languagePackage}}
                      |""".stripMargin)
    title()
    tableColors()
    if data.specializations.isEmpty then defaultPrint() else specializationsPrint()
    legend()
    builder.append("\\end{document}")
    builder
  }

  private def defaultPrint(): Unit = {
    val (mandatory, elective) = entries.partition(_.module.mandatory)
    if mandatory.nonEmpty then {
      examLists(mandatory, messages("latex.exam_lists.mandatory_modules.chapter"))
    }
    if elective.nonEmpty then {
      examLists(elective, messages("latex.exam_lists.elective_modules.chapter"))
    }
  }

  private def tableColors() = {
    builder.append("""% defines table colors
                     |\definecolor{lightgray}{gray}{0.95}
                     |""".stripMargin)
  }

  private def specializationsPrint(): Unit = {
    if entries.isEmpty then return
    examLists(entries, messages("latex.exam_lists.modules.chapter"))
    moduleMatrix(messages("latex.exam_lists.module_matrix.chapter"))
  }

  private val examLabel = messages("latex.exam_lists.table.exam")

  private def assessmentRow(xs: List[ExamListAssessmentMethod]) =
    if xs.isEmpty then "—"
    else
      xs
        .map(m => (m, assessmentMethodsById.get(m.method)))
        .sortBy { case (m, method) => method.fold(m.method)(_.deLabel) }
        .map {
          case (m, method) =>
            val label = escape(strings.label(method))
            m.percentage.fold(label)(d => s"$label~\\mbox{(${fmtDouble(d)} \\%)}")
        }
        .mkString("\\newline ")

  private def examinerName(id: String) =
    escape(
      peopleById(id) match
        case p: Identity.Person         => p.lastname
        case Identity.Group(_, label)   => label
        case Identity.Unknown(_, label) => label
    )

  /** Lists the first examiner above the second one. */
  private def examinerRow(m: ExamListModuleOption) =
    s"${examinerName(m.firstExaminer)}\\newline ${examinerName(m.secondExaminer)}"

  /** Lists one phase per line, followed by an empty line if its registration period takes two lines. */
  private def examPhasesRow(xs: List[String], withPeriods: Boolean) =
    xs.map(id => escape(messages(s"exam_phase.short.$id")))
      .mkString(if withPeriods then "\\newline\\mbox{}\\newline " else "\\newline ")

  /** Renders the period of each phase on two lines: start and end. */
  private def registrationPeriodsRow(phases: List[String], exam: ExamListExam) =
    phases
      .map { phase =>
        val p = exam.registrationPeriods(phase)
        s"${p.start.format(dateFormat)}~–\\newline ${p.end.format(dateFormat)}"
      }
      .mkString("\\newline ")

  private def moduleMatrixRow(entry: PreparedExamListEntry, cols: Seq[String]) = {
    builder.append(escape(entry.module.title))
    cols.foreach { poId =>
      val relations = entry.module.po.filter(p => p.specialization.getOrElse(p.po) == poId)
      val value     =
        if relations.exists(_.mandatory) then {
          usedP = true
          "P"
        } else
          relations.headOption.fold("") { p =>
            p.instanceOf.flatMap(genericModulesById.get) match
              case Some(gm) =>
                genericModuleLegend += gm
                escape(gm.abbrev)
              case None =>
                usedW = true
                "W"
          }
      builder.append(s" & $value")
    }
    builder.append("\\\\\n")
  }

  /**
   * Renders one row per module. Named exams follow in indented rows, so that a module and its exams form one block in
   * one color without a page break inside.
   */
  private def examLists(entries: List[PreparedExamListEntry], titleLabel: String): Unit = {
    val continuationHeaderLabel = messages("latex.exam_lists.table.header.continuation")
    val continuationFooterLabel = messages("latex.exam_lists.table.footer.continuation")

    builder
      .append(s"""\\begin{landscape}
                 |\\section*{$titleLabel}
                 |\\small
                 |\\begin{longtable}{|
                 |$rowWidth
                 |$moduleExamWidth
                 |$assessmentTitleWidth
                 |$examinerWidth
                 |$examPhasesWidth
                 |$registrationPeriodWidth|}
                 |% First page header
                 |\\tablehead
                 |\\endfirsthead
                 |% Continuation page header
                 |\\multicolumn{6}{l}{\\textit{$continuationHeaderLabel}} \\\\
                 |\\tablehead
                 |\\endhead
                 |% Continuation page footer
                 |\\multicolumn{6}{r}{{\\underline{$continuationFooterLabel}}} \\\\
                 |\\endfoot
                 |\\endlastfoot
                 |% table data
                 |""".stripMargin)
    entries.zipWithIndex.foreach { (entry, i) =>
      val m      = entry.module
      val module = List(s"${i + 1}", escape(m.title), assessmentRow(m.assessmentMethods), examinerRow(m))
      val phases = examPhasesRow(entry.examPhases, withPeriods = !m.isGeneric)
      val rows   = entry.exams match {
        case List(ExamListExam(None, _)) | Nil =>
          List(module ++ List(phases, entry.exams.headOption.fold("")(registrationPeriodsRow(entry.examPhases, _))))
        case exams =>
          val count = messages("latex.exam_lists.table.exam_count", exams.size)
          val title = s"${module(1)}\\newline \\textcolor{gray}{$count}"
          (module.updated(1, title) ++ List("", "")) :: exams.map(e =>
            List(
              "",
              s"\\leftskip=1em\\relax \\textcolor{gray}{$examLabel} ${escape(e.name.getOrElse(m.title))}",
              "",
              "",
              phases,
              registrationPeriodsRow(entry.examPhases, e)
            )
          )
      }
      val color = if i % 2 == 1 then "\\rowcolor{lightgray}" else ""
      builder.append(rows.map(color + _.mkString(" & ")).mkString("\\\\*\n")).append("\\\\\n")
    }
    builder.append("""\hline
                     |\end{longtable}
                     |\end{landscape}
                     |""".stripMargin)
  }

  private def moduleMatrix(titleLabel: String) = {
    val remainingWidth          = 0.9 - moduleTitleWidthValue
    val cols: Seq[POShort]      = matrixColumns
    val width                   = Math.max(remainingWidth / cols.size, 0.08)
    val colWidth                = cols.map(_ => s"||Cp{$width\\linewidth}").mkString("\n")
    val colHeader               = cols.map(po => s"& \\textbf{${escape(po._3)}}").mkString("\n")
    val colIds                  = cols.map(_._1)
    val continuationFooterLabel = messages("latex.exam_lists.table.footer.continuation")

    builder
      .append(
        s"""\\begin{landscape}
           |\\rowcolors{1}{lightgray}{}
           |\\section*{$titleLabel}
           |\\small
           |\\begin{longtable}{|\n$moduleTitleWidth\n$colWidth|}
           |\\hline
           |\\textbf{${messages("latex.exam_lists.table.header.moduleName")}}
           |$colHeader
           |\\\\ \\hline \\endhead
           |\\multicolumn{${cols.size + 1}}{r}{{\\underline{$continuationFooterLabel}}} \\\\
           |\\endfoot
           |\\endlastfoot
           |""".stripMargin
      )
    entries.foreach(moduleMatrixRow(_, colIds))
    builder.append("""\hline
                     |\end{longtable}
                     |\end{landscape}
                     |""".stripMargin)
  }

  private def matrixColumns: Seq[POShort] =
    data.specializations
      .map(s => (s.id, s.label, s.abbreviation))
      .prepended((studyProgram.po.id, strings.label(studyProgram), studyProgram.abbreviation))

  private def legend() = {
    builder.append(s"\\newpage\n\\section*{${messages("latex.exam_lists.legend.chapter")}}")
    builder.append(s"\n\\subsection*{${messages("latex.exam_lists.table.header.examPhases")}}\n\\begin{itemize}")
    ExamPhase.all.toList.filterNot(_ == ExamPhase.none).foreach { phase =>
      val id = phase.id
      builder.append(
        s"\n\\item ${escape(messages(s"exam_phase.short.$id"))}: ${escape(messages(s"exam_phase.label.$id"))}"
      )
    }
    builder.append("\n\\end{itemize}")

    if data.specializations.nonEmpty && entries.nonEmpty then {
      builder.append(s"\n\\subsection*{${messages("latex.exam_lists.module_matrix.chapter")}}\n\\begin{itemize}")
      matrixColumns.foreach {
        case (_, label, abbrev) =>
          builder.append(escape(s"\n\\item $abbrev: $label"))
      }
      if usedP then builder.append(s"\n\\item P: ${messages("latex.exam_lists.module_matrix.mandatory_module.label")}")
      if usedW then builder.append(s"\n\\item W: ${messages("latex.exam_lists.module_matrix.elective_module.label")}")
      genericModuleLegend.foreach(m => builder.append(s"\n\\item ${escape(m.abbrev)}: ${escape(m.title)}"))
      builder.append("\n\\end{itemize}")
    }
  }

  private def title() = {
    val semesterLabel     = s"\\LARGE ${strings.label(semester)} ${semester.year}"
    val validityDate      = date.fold("\\rule{4cm}{0.4pt}")(_.format(dateFormat))
    val titleLabel        = messages("latex.exam_lists.title")
    val studyProgramLabel = s"${escape(strings.label(studyProgram))} PO ${studyProgram.po.version}"
    val degreeLabel       = strings.description(studyProgram.degree)
    val validityStatement = messages("latex.exam_lists.validity_statement", validityDate)
    builder.append(
      s"""\\begin{titlepage}
         |\\vspace*{\\fill}
         |\\begin{center}
         |\\Huge $titleLabel \\\\ [1.5ex]
         |\\LARGE $studyProgramLabel \\\\ [1ex]
         |\\LARGE $degreeLabel \\\\ [1ex]
         |$semesterLabel \\\\ [4.5ex]
         |\\large Fakultät für Informatik und Ingenieurwissenschaften \\\\ [1.5ex]
         |\\large \\today
         |\\end{center}
         |\\vspace*{\\fill}
         |$validityStatement
         |\\end{titlepage}
         |""".stripMargin
    )
  }

  private def packages() =
    builder
      .append("""% packages
                |\usepackage[english, ngerman]{babel}
                |\usepackage[a4paper, left=1cm, right=1cm, top=1cm, bottom=1cm]{geometry}
                |\usepackage{lscape} % landscape
                |\usepackage{longtable} % page break tables
                |\usepackage[table]{xcolor} % table color
                |\usepackage{pdflscape} % automatically rotates pdf page where lscape is used
                |""".stripMargin)
      .appendOpt(
        Option.when(isPreview)(
          s"\\usepackage[colorspec=0.9,text=${strings.previewLabel}]{draftwatermark}\n"
        )
      )

  private def commands() = {
    builder.append("""% commands and settings
                     |\setlength{\marginparwidth}{0pt} % no margin notes
                     |\setlength{\marginparsep}{0pt} % no margin notes
                     |\setlength{\parindent}{0pt} % removes paragraph indentation
                     |\setlength{\parskip}{.8em} % adds space between paragraphs
                     |% empty page style (no headers or footers)
                     |\pagestyle{empty}
                     |\makeatletter
                     |\let\ps@plain\ps@empty
                     |\makeatother
                     |% customize table
                     |\newcolumntype{L}{>{\raggedright\arraybackslash}}
                     |\newcolumntype{R}{>{\raggedleft\arraybackslash}}
                     |\newcolumntype{C}{>{\centering\arraybackslash}}
                     |\renewcommand{\arraystretch}{1.5} % adds line spacing to table cells
                     |""".stripMargin)
    builder.append(s"""\\newcommand{\\tablehead}{%
                      |\\hline
                      |\\rowcolor{lightgray}
                      |\\textbf{Nr.}
                      |& \\textbf{${messages("latex.exam_lists.table.header.moduleExam")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.assessmentMethod")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.examiner")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.examPhases")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.registrationPeriod")}}
                      |\\\\ \\hline
                      |}
                      |""".stripMargin)
  }
}
