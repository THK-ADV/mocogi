package printing.latex

import catalog.Semester
import cats.data.NonEmptyList
import controllers.LangOps
import models.*
import models.core.AssessmentMethod
import models.core.Identity
import ops.StringBuilderOps.SBOps
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import printing.fmtDouble
import printing.IDLabelDescOps
import printing.LabelOps
import printing.PrintingLanguage

object ExamListsLatexPrinter {
  def preview(
      modules: Seq[ModuleProtocol],
      studyProgram: StudyProgramView,
      assessmentMethods: Seq[AssessmentMethod],
      people: Seq[Identity],
      messages: MessagesApi,
      lang: Lang
  ): ExamListsLatexPrinter =
    new ExamListsLatexPrinter(modules, studyProgram, assessmentMethods, people, None, messages)(
      using lang
    )
}

final class ExamListsLatexPrinter(
    modules: Seq[ModuleProtocol],
    studyProgram: StudyProgramView,
    assessmentMethods: Seq[AssessmentMethod],
    people: Seq[Identity],
    semester: Option[Semester],
    messages: MessagesApi,
)(using lang: Lang) {

  private val builder: StringBuilder = new StringBuilder()

  given printingLang: PrintingLanguage = lang.toPrintingLang()

  def print() = {
    builder.append("\\documentclass[article, 11pt, oneside]{book}\n")
    packages(true)
    commands()
    builder.append(s"""\\begin{document}
                      |\\selectlanguage{${messages("latex.lang.package_name")}}
                      |""".stripMargin)
    title()
    builder.append("""% defines table colors
                     |\definecolor{lightgray}{gray}{0.95}
                     |\rowcolors{1}{lightgray}{}
                     |""".stripMargin)
    val (mandatory, elective) = splitModules()
    builder.append(s"""\\chapter{${messages("latex.exam_lists.mandatory_modules.chapter")}}
                      |\\newpage
                      |""".stripMargin)
    examLists(mandatory, _.mandatory)
    builder.append(s"""\\chapter{${messages("latex.exam_lists.elective_modules.chapter")}}
                      |\\newpage
                      |""".stripMargin)
    examLists(elective, a => if a.optional.isEmpty then a.mandatory else a.optional)
    builder.append("\\end{document}")
    builder
  }

  private def assessmentRow(xs: List[ModuleAssessmentMethodEntryProtocol]) =
    if xs.isEmpty then messages("latex.exam_lists.no-assessment.label")
    else
      xs
        .sortBy(_.method)
        .map { m =>
          val label = assessmentMethods.find(_.id == m.method).get.localizedLabel
          escape(m.percentage.fold(label)(d => s"$label (${fmtDouble(d)} %)"))
        }
        .mkString(", ")

  private def examinerRow(id: String) =
    escape(people.find(_.id == id).get.fullName)

  private def examPhasesRow(xs: NonEmptyList[String]) =
    xs.sorted
      .map(id => escape(messages(s"exam_phase.short.$id")))
      .toList
      .mkString(", ")

  private def titleRow(title: String) =
    escape(title)

  private def moduleRow(
      row: Int,
      title: String,
      assessmentMethods: List[ModuleAssessmentMethodEntryProtocol],
      examPhases: NonEmptyList[String],
      examiner: Examiner.ID
  ) = {
    builder.append(s"${row + 1}")
    builder.append(s" & ${this.titleRow(title)}")
    builder.append(s" & ${this.assessmentRow(assessmentMethods)}")
    builder.append(s" & ${this.examinerRow(examiner.first)}")
    builder.append(s" & ${this.examinerRow(examiner.second)}")
    builder.append(s" & ${this.examPhasesRow(examPhases)}")
    builder.append("\\\\\n")
  }

  private def parentModuleRow(row: Int, title: String) = {
    builder.append(s"${row + 1}")
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

  private def examLists(
      modules: Seq[ModuleProtocol],
      moduleAssessmentMethodEntries: ModuleAssessmentMethodsProtocol => List[ModuleAssessmentMethodEntryProtocol]
  ): Unit = {
    if modules.isEmpty then return
    builder
      .append(s"""\\begin{landscape}
                 |\\centering
                 |\\large
                 |\\begin{longtable}{|
                 |Lp{.025\\linewidth}
                 |Lp{.25\\linewidth}
                 |Lp{.25\\linewidth}
                 |Lp{.15\\linewidth}
                 |Lp{.15\\linewidth}
                 |Lp{.15\\linewidth}|}
                 |\\hline
                 |\\textbf{Nr.}
                 |& \\textbf{${messages("latex.exam_lists.table.header.moduleName")}}
                 |& \\textbf{${messages("latex.exam_lists.table.header.assessmentMethod")}}
                 |& \\textbf{${messages("latex.exam_lists.table.header.firstExaminer")}}
                 |& \\textbf{${messages("latex.exam_lists.table.header.secondExaminer")}}
                 |& \\textbf{${messages("latex.exam_lists.table.header.examPhases")}}
                 |\\\\ \\hline \\endhead
                 |\\multicolumn{6}{r}{{\\underline{${messages("latex.exam_lists.table.footer.nextPageHint")}}}} \\\\
                 |\\endfoot
                 |\\endlastfoot
                 |""".stripMargin)
    modules
      .sortBy(_.metadata.title)
      .zipWithIndex
      .foreach {
        case (m, i) =>
          m.metadata.moduleRelation match {
            case Some(ModuleRelationProtocol.Child(_)) => // child modules are rendered below their parent module
            case Some(ModuleRelationProtocol.Parent(children)) =>
              parentModuleRow(i, m.metadata.title)
              children.toList.map(id => modules.find(_.id.contains(id)).get).sortBy(_.metadata.title).foreach { m =>
                childModuleRow(
                  m.metadata.title,
                  moduleAssessmentMethodEntries(m.metadata.assessmentMethods),
                  m.metadata.examPhases,
                  m.metadata.examiner
                )
              }
            case None =>
              moduleRow(
                i,
                m.metadata.title,
                moduleAssessmentMethodEntries(m.metadata.assessmentMethods),
                m.metadata.examPhases,
                m.metadata.examiner
              )
          }
      }
    builder.append("""\hline
                     |\end{longtable}
                     |\end{landscape}
                     |""".stripMargin)
  }

  private def title() = {
    val titleLabel = messages("latex.exam_lists.title")
    val studyProgramLabel =
      s"${escape(studyProgram.localizedLabel(studyProgram.specialization))} PO ${studyProgram.po.version}"
    builder.append(
      s"""\\title{
         |\\Huge $titleLabel \\\\ [1.5ex]
         |\\LARGE $studyProgramLabel \\\\ [1ex]
         |\\LARGE ${studyProgram.degree.localizedDesc} \\\\ [1ex]
         |${semester.fold(messages("latex.preview_label"))(s => s"\\LARGE ${s.localizedLabel} ${s.year}\n")}}
         |\\author{TH Köln, Campus Gummersbach}
         |\\date{\\today}
         |\\maketitle
         |""".stripMargin
    )
  }

  private def packages(draft: Boolean) =
    builder
      .append("""% packages
                |\usepackage[english, ngerman]{babel}
                |\usepackage[a4paper, total={16cm, 24cm}, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}
                |\usepackage{fancyhdr} % customize the page header
                |\usepackage{parskip} % customize paragraph style
                |\usepackage{titlesec} % customize chapter style
                |\usepackage{lscape} % landscape
                |\usepackage{longtable} % page break tables
                |\usepackage[table]{xcolor} % table color
                |\usepackage{pdflscape} % automatically rotates pdf page where lscape is used
                |""".stripMargin)
      .appendOpt(
        Option.when(draft)(
          s"\\usepackage[colorspec=0.9,text=${messages("latex.preview_label")}]{draftwatermark}\n"
        )
      )

  private def commands() =
    builder.append("""% commands and settings
                     |\providecommand{\tightlist}{\setlength{\itemsep}{0pt}\setlength{\parskip}{0pt}}
                     |% customize the page style
                     |\pagestyle{fancy}
                     |\fancyhf{} % clears header and footer
                     |\renewcommand{\headrulewidth}{0pt} % removes header rule
                     |\fancyfoot[C]{\thepage} % adds page number to the center of the footer
                     |\fancyfoot[L]{\nouppercase{\leftmark}} % adds chapter to the left of the footer
                     |\setlength{\parindent}{0pt} % set paragraph indentation to zero
                     |\setlength{\parskip}{0.5\baselineskip} % sets vertical space between paragraphs
                     |\setlength{\marginparwidth}{0pt} % no margin notes
                     |\setlength{\marginparsep}{0pt} % no margin notes
                     |% customize table
                     |\newcolumntype{L}{>{\raggedright\arraybackslash}}
                     |\newcolumntype{R}{>{\raggedleft\arraybackslash}}
                     |\newcolumntype{C}{>{\centering\arraybackslash}}
                     |\renewcommand{\chaptermark}[1]{\markboth{#1}{}} % removes chapter label of the footer
                     |\renewcommand{\arraystretch}{2.0} % adds line spacing to table cells
                     |% define the chapter format
                     |\titleformat{\chapter}[display]
                     |{\normalfont\Huge\bfseries} % font attributes
                     |{\vspace*{\fill}} % vertical space before the chapter title
                     |{0pt} % horizontal space between the chapter title and the left margin
                     |{\Huge\centering} % font size of the chapter title
                     |[\vspace*{\fill}] % vertical space after the chapter title
                     |""".stripMargin)

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
