package printing.latex

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import scala.collection.mutable.ListBuffer

import cats.data.NonEmptyList
import models.*
import models.core.AssessmentMethod
import models.core.ExamPhases
import models.core.ExamPhases.ExamPhase
import models.core.Identity
import models.core.Specialization
import ops.StringBuilderOps.SBOps
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.fmtDouble
import printing.LocalizedStrings

object ExamListsLatexPrinter {
  def default(
      modules: Seq[ModuleProtocol],
      studyProgram: StudyProgramView,
      assessmentMethods: Seq[AssessmentMethod],
      people: Seq[Identity],
      specializations: Seq[Specialization],
      genericModules: Seq[ModuleCore],
      semester: Semester,
      timestamp: LocalDate,
      messages: MessagesApi,
      lang: Lang
  ): ExamListsLatexPrinter =
    new ExamListsLatexPrinter(
      modules,
      studyProgram,
      assessmentMethods,
      people,
      specializations,
      genericModules,
      Some(semester, timestamp),
      messages
    )(
      using lang
    )

  def preview(
      modules: Seq[ModuleProtocol],
      studyProgram: StudyProgramView,
      assessmentMethods: Seq[AssessmentMethod],
      people: Seq[Identity],
      specializations: Seq[Specialization],
      genericModules: Seq[ModuleCore],
      messages: MessagesApi,
      lang: Lang
  ): ExamListsLatexPrinter =
    new ExamListsLatexPrinter(
      modules,
      studyProgram,
      assessmentMethods,
      people,
      specializations,
      genericModules,
      None,
      messages
    )(
      using lang
    )
}

final class ExamListsLatexPrinter(
    modules: Seq[ModuleProtocol],
    studyProgram: StudyProgramView,
    assessmentMethods: Seq[AssessmentMethod],
    people: Seq[Identity],
    specializations: Seq[Specialization],
    genericModules: Seq[ModuleCore],
    semester: Option[(Semester, LocalDate)],
    messages: MessagesApi,
)(using lang: Lang)
    extends Logging {

  // (ID, Label, Abbreviation)
  opaque type POShort = (String, String, String)

  private val builder: StringBuilder = new StringBuilder()

  private val strings = new LocalizedStrings(messages)(using lang)

  private val rowWidth = "Lp{.02\\linewidth}"

  private val moduleTitleWidthValue = .255

  private val moduleTitleWidth = s"Lp{$moduleTitleWidthValue\\linewidth}"

  private val assessmentTitleWidth = "Lp{.25\\linewidth}"

  private val examinerWidth = "Lp{.125\\linewidth}"

  private val examPhasesWidth = "Lp{.125\\linewidth}"

  private val genericModuleLegend = scala.collection.mutable.Set[ModuleCore]()

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
    if specializations.isEmpty then defaultPrint() else specializationsPrint()
    builder.append("\\end{document}")
    builder
  }

  private def defaultPrint(): Unit = {
    val (mandatory, elective) = splitModules()
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
    if modules.isEmpty then return
    examLists(modules, messages("latex.exam_lists.modules.chapter"))
    moduleMatrix(messages("latex.exam_lists.module_matrix.chapter"))
  }

  private def assessmentRow(xs: List[ModuleAssessmentMethodEntryProtocol]) =
    if xs.isEmpty then messages("latex.exam_lists.no-assessment.label")
    else
      xs
        .sortBy(_.method)
        .map { m =>
          val label = strings.label(assessmentMethods.find(_.id == m.method))
          escape(m.percentage.fold(label)(d => s"$label (${fmtDouble(d)} %)"))
        }
        .mkString(", ")

  private def examinerRow(id: String) =
    escape(
      people.find(_.id == id).get match
        case Identity.Person(_, lastname, firstname, _, _, _, _, _, _, _) => s"${firstname.head}. $lastname"
        case Identity.Group(_, label)                                     => label
        case Identity.Unknown(_, label)                                   => label
    )

  private def examPhasesRow(xs: NonEmptyList[String]) = {
    val sb         = new StringBuilder()
    val examPhases = ListBuffer.from(xs.toList)
    examPhases.find(_ == ExamPhase.sose1.id).zip(examPhases.find(_ == ExamPhase.sose2.id)).foreach {
      case (sose1, sose2) =>
        sb.append(escape(s"${messages("exam_phase.short.sose")} 1 & 2"))
        examPhases.subtractOne(sose1)
        examPhases.subtractOne(sose2)
    }
    examPhases.find(_ == ExamPhase.offSose.id).zip(examPhases.find(_ == ExamPhase.offWise.id)).foreach {
      case (offSose, offWise) =>
        val str = escape(
          s"${messages("exam_phase.short.off")} ${messages("exam_phase.short.sose")} & ${messages("exam_phase.short.wise")}"
        )
        if sb.isEmpty then sb.append(str) else sb.append(s", $str")
        examPhases.subtractOne(offSose)
        examPhases.subtractOne(offWise)
    }
    examPhases.foreach { id =>
      val str = escape(messages(s"exam_phase.short.$id"))
      if sb.isEmpty then sb.append(str) else sb.append(s", $str")
    }
    sb.result()
  }

  private def titleRow(title: String) =
    escape(title)

  private def moduleRow(
      row: Int,
      title: String,
      assessmentMethods: List[ModuleAssessmentMethodEntryProtocol],
      examPhases: NonEmptyList[String],
      examiner: Examiner.ID
  ) = {
    builder.append(s"$row")
    builder.append(s" & ${this.titleRow(title)}")
    builder.append(s" & ${this.assessmentRow(assessmentMethods)}")
    builder.append(s" & ${this.examinerRow(examiner.first)}")
    builder.append(s" & ${this.examinerRow(examiner.second)}")
    builder.append(s" & ${this.examPhasesRow(examPhases)}")
    builder.append("\\\\\n")
  }

  private def parentModuleRow(row: Int, title: String) = {
    builder.append(s"$row")
    builder.append(s" & ${this.titleRow(title)}")
    builder.append(" &")
    builder.append(" &")
    builder.append(" &")
    builder.append(" &")
    builder.append("\\\\\n")
  }

  private def childModuleRow(
      title: String,
      assessmentMethods: List[ModuleAssessmentMethodEntryProtocol],
      examPhases: NonEmptyList[String],
      examiner: Examiner.ID
  ) = {
    builder.append(s" & ${this.titleRow(title)}")
    builder.append(s" & ${this.assessmentRow(assessmentMethods)}")
    builder.append(s" & ${this.examinerRow(examiner.first)}")
    builder.append(s" & ${this.examinerRow(examiner.second)}")
    builder.append(s" & ${this.examPhasesRow(examPhases)}")
    builder.append("\\\\\n")
  }

  private def moduleNotFoundRow(id: UUID) = {
    builder.append(s" & ${escape(id.toString)}")
    builder.append(" & & & &")
    builder.append("\\\\\n")
  }

  private def moduleMatrixRow(title: String, po: ModulePOProtocol, cols: Seq[String], moduleId: => UUID) = {
    builder.append(this.titleRow(title))
    cols.foreach { poId =>
      var value           = ""
      val mandatoryExists = po.mandatory.exists(_.fullPo == poId)
      val electiveExists  = po.optional.find(_.fullPo == poId)
      if mandatoryExists && electiveExists.isDefined then
        logger.error(
          s"module $moduleId should have either a mandatory or elective po relationship to $poId, but has both"
        )
      if mandatoryExists then {
        value += "P"
        usedP = true
      }
      electiveExists.foreach { p =>
        genericModules.find(_.id == p.instanceOf) match
          case Some(gm) =>
            value += gm.abbrev
            genericModuleLegend += gm
          case None =>
            logger.error(s"expected generic module ${p.instanceOf} to exists")
            value += "W"
            usedW = true
      }
      if value.isEmpty then builder.append(" & ") else builder.append(s" & $value")
    }
    builder.append("\\\\\n")
  }

  private def examLists(modules: Seq[ModuleProtocol], titleLabel: String): Unit = {
    val continuationHeaderLabel = messages("latex.exam_lists.table.header.continuation")
    val continuationFooterLabel = messages("latex.exam_lists.table.footer.continuation")

    builder
      .append(s"""\\begin{landscape}
                 |\\rowcolors{1}{lightgray}{}
                 |\\section*{$titleLabel}
                 |\\begin{longtable}{|
                 |$rowWidth
                 |$moduleTitleWidth
                 |$assessmentTitleWidth
                 |$examinerWidth
                 |$examinerWidth
                 |$examPhasesWidth|}
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
    var row = 1
    modules
      .sortBy(_.metadata.title)
      .foreach { m =>
        m.metadata.moduleRelation match {
          case Some(ModuleRelationProtocol.Child(_)) => // child modules are rendered below their parent module
          case Some(ModuleRelationProtocol.Parent(children)) =>
            parentModuleRow(row, m.metadata.title)
            row += 1
            children.toList
              .map(id => modules.find(_.id.contains(id)).toRight(id))
              .sortBy(_.fold(_.toString, _.metadata.title))
              .foreach {
                case Right(m) =>
                  childModuleRow(
                    m.metadata.title,
                    m.metadata.assessmentMethods.mandatory,
                    m.metadata.examPhases,
                    m.metadata.examiner
                  )
                case Left(id) =>
                  logger.error(s"expected child module $id to exists")
                  moduleNotFoundRow(id)
              }
          case None =>
            moduleRow(
              row,
              m.metadata.title,
              m.metadata.assessmentMethods.mandatory,
              m.metadata.examPhases,
              m.metadata.examiner
            )
            row += 1
        }
      }
    builder.append("""\hline
                     |\end{longtable}
                     |\end{landscape}
                     |""".stripMargin)
  }

  private def moduleMatrix(titleLabel: String) = {
    val remainingWidth = 0.9 - moduleTitleWidthValue
    val cols: Seq[POShort] = specializations
      .map(s => (s.id, s.label, s.abbreviation))
      .prepended((studyProgram.po.id, strings.label(studyProgram), studyProgram.abbreviation))
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
    modules
      .sortBy(_.metadata.title)
      .foreach { m =>
        m.metadata.moduleRelation match {
          case Some(ModuleRelationProtocol.Child(_)) => // child modules are rendered below their parent module
          case Some(ModuleRelationProtocol.Parent(children)) =>
            children.toList
              .map { id =>
                val child = modules.find(_.id.contains(id))
                if child.isEmpty then
                  logger.error(s"error while printing parent module ${m.id.get}: unable to find child module $id")
                child
              }
              .collect { case Some(m) => m }
              .sortBy(_.metadata.title)
              .foreach { m =>
                moduleMatrixRow(m.metadata.title, m.metadata.po, colIds, m.id.get)
              }
          case None =>
            moduleMatrixRow(m.metadata.title, m.metadata.po, colIds, m.id.get)
        }
      }
    builder.append("""\hline
                     |\end{longtable}
                     |\end{landscape}
                     |\newpage""".stripMargin)
    legend(cols)
  }

  private def legend(cols: Seq[POShort]) = {
    builder.append("""\section*{Legende}
                     |\begin{itemize}""".stripMargin)
    cols.foreach {
      case (_, label, abbrev) =>
        builder.append(escape(s"\n\\item $abbrev: $label"))
    }
    if usedP then builder.append(s"\n\\item P: ${messages("latex.exam_lists.module_matrix.mandatory_module.label")}")
    if usedW then builder.append(s"\n\\item W: ${messages("latex.exam_lists.module_matrix.elective_module.label")}")
    genericModuleLegend.foreach(m => builder.append(s"\n\\item ${m.abbrev}: ${m.title}"))
    builder.append("\n\\end{itemize}")
  }

  private def title() = {
    val (semesterLabel, validityStatementDate) = semester match {
      case Some((s, d)) =>
        val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        (s"\\LARGE ${strings.label(s)} ${s.year}", d.format(df))
      case None =>
        (strings.previewLabel, "\\rule{4cm}{0.4pt}")
    }
    val titleLabel        = messages("latex.exam_lists.title")
    val studyProgramLabel = s"${escape(strings.label(studyProgram))} PO ${studyProgram.po.version}"
    val degreeLabel       = strings.description(studyProgram.degree)
    val validityStatement = messages("latex.exam_lists.validity_statement", validityStatementDate)
    builder.append(
      s"""\\begin{titlepage}
         |\\vspace*{\\fill}
         |\\begin{center}
         |\\Huge $titleLabel \\\\ [1.5ex]
         |\\LARGE $studyProgramLabel \\\\ [1ex]
         |\\LARGE $degreeLabel \\\\ [1ex]
         |\\LARGE $semesterLabel \\\\ [4.5ex]
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
        Option.when(semester.isEmpty)(
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
                      |\\textbf{Nr.}
                      |& \\textbf{${messages("latex.exam_lists.table.header.moduleName")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.assessmentMethod")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.firstExaminer")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.secondExaminer")}}
                      |& \\textbf{${messages("latex.exam_lists.table.header.examPhases")}}
                      |\\\\ \\hline
                      |}
                      |""".stripMargin)
  }

  private def splitModules(): (Seq[ModuleProtocol], Seq[ModuleProtocol]) =
    modules.partition { m =>
      val mandatory = m.metadata.po.mandatory.exists { a =>
        a.po == studyProgram.po.id && a.specialization
          .zip(studyProgram.specialization)
          .fold(true)(a => a._1 == a._2.id)
      }
      val elective = m.metadata.po.optional.exists { a =>
        a.po == studyProgram.po.id && a.specialization
          .zip(studyProgram.specialization)
          .fold(true)(a => a._1 == a._2.id)
      }
      (mandatory, elective) match {
        case (true, false) => true
        case (false, true) => false
        case (false, false) =>
          assert(false, s"module ${m.id.fold(m.metadata.title)(_.toString)} needs to be either mandatory or elective")
        case (true, true) =>
          assert(
            false,
            s"module ${m.id.fold(m.metadata.title)(_.toString)} must be either mandatory or elective but not both"
          )
      }
    }
}
