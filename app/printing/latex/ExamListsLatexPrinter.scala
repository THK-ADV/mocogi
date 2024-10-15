package printing.latex

import javax.inject.Inject
import javax.inject.Singleton

import catalog.Semester
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

@Singleton
final class ExamListsLatexPrinter @Inject() (messages: MessagesApi) {

  def preview(
      modules: Seq[ModuleProtocol],
      studyProgram: StudyProgramView,
      assessmentMethods: Seq[AssessmentMethod],
      people: Seq[Identity]
  )(using Lang): StringBuilder =
    print(modules, studyProgram, assessmentMethods, people, None)

  private def print(
      modules: Seq[ModuleProtocol],
      studyProgram: StudyProgramView,
      assessmentMethods: Seq[AssessmentMethod],
      people: Seq[Identity],
      semester: Option[Semester]
  )(
      using lang: Lang
  ) = {
    given printingLang: PrintingLanguage = lang.toPrintingLang()
    given builder: StringBuilder         = new StringBuilder()
    builder.append("\\documentclass[article, 11pt, oneside]{book}\n")
    packages(true)
    commands
    builder.append(s"""\\begin{document}
                      |\\selectlanguage{${messages("latex.lang.package_name")}}
                      |""".stripMargin)
    title(studyProgram, semester)
    builder.append("""% defines table colors
                     |\definecolor{lightgray}{gray}{0.95}
                     |\rowcolors{1}{lightgray}{}
                     |""".stripMargin)
    val (mandatory, elective) = splitModules(modules, studyProgram)
    builder.append(s"""\\chapter{${messages("latex.exam_lists.mandatory_modules.chapter")}}
                      |\\newpage
                      |""".stripMargin)
    examLists(mandatory, assessmentMethods, people, _.mandatory)
    builder.append(s"""\\chapter{${messages("latex.exam_lists.elective_modules.chapter")}}
                      |\\newpage
                      |""".stripMargin)
    examLists(elective, assessmentMethods, people, a => if a.optional.isEmpty then a.mandatory else a.optional)
    builder.append("\\end{document}")
    builder
  }

  private def examLists(
      modules: Seq[ModuleProtocol],
      assessmentMethods: Seq[AssessmentMethod],
      people: Seq[Identity],
      moduleAssessmentMethodEntries: ModuleAssessmentMethodsProtocol => List[ModuleAssessmentMethodEntryProtocol]
  )(
      using lang: Lang,
      pLang: PrintingLanguage,
      builder: StringBuilder
  ): Unit = {
    if modules.isEmpty then return
    val conjunctionSeparator = s" ${messages("latex.exam_lists.logical.conjunction")} "
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
          val assessmentRow = moduleAssessmentMethodEntries(m.metadata.assessmentMethods)
            .sortBy(_.method)
            .map { m =>
              val label = assessmentMethods.find(_.id == m.method).get.localizedLabel
              escape(m.percentage.fold(label)(d => s"$label (${fmtDouble(d)} %)"))
            }
            .mkString(conjunctionSeparator)
          val examinerRow = (id: String) => escape(people.find(_.id == id).get.fullName)
          val examPhases = m.metadata.examPhases.sorted
            .map(id => escape(messages(s"exam_phase.short.$id")))
            .toList
            .mkString(conjunctionSeparator)
          builder.append(s"${i + 1}")
          builder.append(s" & ${escape(m.metadata.title)}")
          builder.append(s" & $assessmentRow")
          builder.append(s" & ${examinerRow(m.metadata.examiner.first)}")
          builder.append(s" & ${examinerRow(m.metadata.examiner.second)}")
          builder.append(s" & $examPhases")
          builder.append("\\\\\n")
      }
    builder.append("""\hline
                     |\end{longtable}
                     |\end{landscape}
                     |""".stripMargin)
  }

  private def title(
      studyProgram: StudyProgramView,
      semester: Option[Semester]
  )(using pLang: PrintingLanguage, lang: Lang, builder: StringBuilder) = {
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

  private def packages(draft: Boolean)(using lang: Lang, builder: StringBuilder) =
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
                |""".stripMargin)
      .appendOpt(
        Option.when(draft)(
          s"\\usepackage[colorspec=0.9,text=${messages("latex.preview_label")}]{draftwatermark}\n"
        )
      )

  private def commands(using builder: StringBuilder) =
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

  private def splitModules(
      modules: Seq[ModuleProtocol],
      studyProgram: StudyProgramView
  ): (Seq[ModuleProtocol], Seq[ModuleProtocol]) =
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
