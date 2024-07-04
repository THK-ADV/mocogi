package printing.latex

import catalog.Semester
import models._
import models.core._
import monocle.Lens
import monocle.macros.GenLens
import ops.StringBuilderOps.SBOps
import parsing.types.ModuleContent
import play.api.Logging
import printing.pandoc.PandocApi
import printing.{
  IDLabelDescOps,
  LabelOps,
  PrintingLanguage,
  fmtCommaSeparated,
  fmtDouble,
  fmtIdentity
}

import java.util.UUID
import javax.inject.{Inject, Singleton}

/** Style from: https://www.overleaf.com/learn/latex/Page_size_and_margins
  */
@Singleton
final class ModuleCatalogLatexPrinter @Inject() (pandocApi: PandocApi)
    extends Logging {

  private implicit def identityOrd: Ordering[Identity] =
    Ordering
      .by[Identity, Int] {
        case _: Identity.Person  => 1
        case _: Identity.Group   => 2
        case _: Identity.Unknown => 3
      }
      .orElse(Ordering.by[Identity, String] {
        case p: Identity.Person  => p.lastname
        case g: Identity.Group   => g.id
        case u: Identity.Unknown => u.id
      })
      .orElse(Ordering.by[Identity, String] {
        case p: Identity.Person  => p.firstname
        case g: Identity.Group   => g.id
        case u: Identity.Unknown => u.id
      })

  def print(
      studyProgram: StudyProgramView,
      semester: Option[Semester],
      entries: Seq[ModuleProtocol],
      moduleTypes: Seq[ModuleType],
      languages: Seq[ModuleLanguage],
      seasons: Seq[Season],
      people: Seq[Identity],
      assessmentMethods: Seq[AssessmentMethod],
      studyProgramViews: Seq[StudyProgramView],
      diffs: Seq[(ModuleCore, Set[String])] = Seq.empty
  )(implicit lang: PrintingLanguage): StringBuilder = {
    implicit val builder: StringBuilder = new StringBuilder()
    builder.append("\\documentclass[article, 11pt, oneside]{book}\n")
    packages(semester.isEmpty)
    commands
    builder.append("\\begin{document}\n")
    builder.append(s"\\selectlanguage{${lang.fold("ngerman", "english")}}\n")
    title(studyProgram, semester)
    builder.append("\\maketitle\n")
    newPage
    builder.append("\\layout*\n")
    newPage
    renderDiffs(diffs)
    builder.append("\\tableofcontents\n")
    headlineFormats
    chapter(lang.prologHeadline)
    newPage
    chapter(lang.moduleHeadline)
    newPage
    modules(
      studyProgram.po.id,
      entries.filter(_.metadata.po.mandatory.exists { a =>
        a.po == studyProgram.po.id && a.specialization
          .zip(studyProgram.specialization)
          .fold(true)(a => a._1 == a._2.id)
      }),
      moduleTypes,
      languages,
      seasons,
      people,
      assessmentMethods,
      studyProgramViews
    )
    chapter(lang.studyPlanHeadline)
    builder.append("\\end{document}")
  }

  private def renderDiffs(
      diffs: Seq[(ModuleCore, Set[String])]
  )(implicit builder: StringBuilder) = {
    if (diffs.nonEmpty) {
      builder.append(s"\\textbf{Module Diffs}\n\n")
      diffs.foreach { case (module, diffs) =>
        builder.append(s"${module.title} (${module.id})\n")
        builder.append("\\begin{itemize}\n")
        diffs.foreach(d => builder.append(s"\\item $d\n"))
        builder.append("\\end{itemize}\n")
      }
      newPage
    }
  }

  private def modules(
      po: String,
      entries: Seq[ModuleProtocol],
      moduleTypes: Seq[ModuleType],
      languages: Seq[ModuleLanguage],
      seasons: Seq[Season],
      people: Seq[Identity],
      assessmentMethods: Seq[AssessmentMethod],
      studyProgramViews: Seq[StudyProgramView]
  )(implicit lang: PrintingLanguage, builder: StringBuilder) = {
    def row(key: String, value: String) =
      builder.append(s"$key & $value \\\\\n")

    def content(
        id: Option[UUID],
        deContent: ModuleContent,
        enContent: ModuleContent,
        entries: List[(String, Lens[ModuleContent, String])]
    ): Unit = {
      val markdownContent = new StringBuilder()
      entries.foreach { case (headline, lens) =>
        markdownContent.append(s"## $headline\n")
        val content = lang.fold(lens.get(deContent), lens.get(enContent))
        if (content.nonEmpty && !content.forall(_.isWhitespace))
          markdownContent.append(content)
        else markdownContent.append(lang.noneLabel)
        markdownContent.append("\n\n")
      }
      pandocApi.toLatex(markdownContent.toString()) match {
        case Left((e, stdErr)) =>
          logger.error(
            s"""content conversation from markdown to latex failed on $id:
               |  - throwable: ${e.getMessage}
               |  - sdtErr: $stdErr""".stripMargin
          )
          builder.append("ERROR\n\n")
        case Right(text) => builder.append(text)
      }
    }

    def go(module: ModuleProtocol): Unit = {
      val (workload, contactHour, selfStudy) =
        lang.workload(module.metadata.workload)
      val poMandatory =
        if (module.metadata.po.mandatory.size == 1) lang.noneLabel
        else
          module.metadata.po.mandatory
            .sortBy(_.po)
            .collect {
              case p if p.po != po =>
                val builder = new StringBuilder()
                val studyProgram = studyProgramViews.find(_.po.id == p.po).get
                val spLabel = escape(
                  studyProgram.localizedLabel(studyProgram.specialization)
                )
                builder
                  .append(
                    s"${studyProgram.degree.localizedLabel}: "
                  )
                  .append(spLabel)
                  .append(s" PO ${studyProgram.po.version}")
                if (p.recommendedSemester.nonEmpty) {
                  builder.append(
                    s" (Semester ${fmtCommaSeparated(p.recommendedSemester)(_.toString())})"
                  )
                }
                builder.toString()
            }
            .mkString("\\newline ")

      section(module.metadata.title)
      builder.append(
        "\\begin{tabularx}{\\linewidth}{@{}>{\\bfseries}l@{\\hspace{.5em}}X@{}}\n"
      )
      row("ID", module.id.fold("Unknown ID")(_.toString))
      row(lang.moduleCodeLabel, escape(module.metadata.abbrev))
      row(lang.moduleTitleLabel, escape(module.metadata.title))
      row(
        lang.moduleTypeLabel,
        moduleTypes
          .find(_.id == module.metadata.moduleType)
          .get
          .localizedLabel
      )
      row(lang.ectsLabel, fmtDouble(module.metadata.ects))
      row(
        lang.languageLabel,
        languages
          .find(_.id == module.metadata.language)
          .get
          .localizedLabel
      )
      row(lang.durationLabel, lang.durationValue(module.metadata.duration))
      row(
        lang.frequencyLabel,
        seasons.find(_.id == module.metadata.season).get.localizedLabel
      )
      row(
        lang.moduleCoordinatorLabel,
        fmtCommaSeparated(
          people
            .filter(p => module.metadata.moduleManagement.exists(_ == p.id))
            .sorted,
          "\\newline "
        )(fmtIdentity)
      )
      row(
        lang.lecturersLabel,
        fmtCommaSeparated(
          people
            .filter(p => module.metadata.lecturers.exists(_ == p.id))
            .sorted,
          "\\newline "
        )(fmtIdentity)
      )
      row(
        lang.assessmentMethodLabel,
        fmtCommaSeparated(
          module.metadata.assessmentMethods.mandatory.sortBy(_.method),
          "\\newline "
        ) { a =>
          val method = assessmentMethods
            .find(_.id == a.method)
            .get
            .localizedLabel
          a.percentage.fold(method)(d => s"$method (${fmtDouble(d)} \\%)")
        }
      )
      row(workload._1, workload._2)
      row(contactHour._1, contactHour._2)
      row(selfStudy._1, selfStudy._2)
      row(
        lang.poLabelShort,
        poMandatory
      )
      builder.append("\\end{tabularx}\n")
      content(
        module.id,
        module.deContent,
        module.enContent,
        List(
          (
            lang.learningOutcomeLabel,
            GenLens[ModuleContent](_.learningOutcome)
          ),
          (lang.moduleContentLabel, GenLens[ModuleContent](_.content)),
          (
            lang.teachingAndLearningMethodsLabel,
            GenLens[ModuleContent](_.teachingAndLearningMethods)
          ),
          (
            lang.recommendedReadingLabel,
            GenLens[ModuleContent](_.recommendedReading)
          ),
          (lang.particularitiesLabel, GenLens[ModuleContent](_.particularities))
        )
      )
    }

    if (entries.isEmpty) newPage
    else
      entries
        .sortBy(_.metadata.title)
        .foreach { e =>
          go(e)
          newPage
        }
  }

  private def escape(str: String) = {
    val buf = new StringBuilder(str.length)
    str.foreach {
      case '_' => buf.append("\\_")
      case '&' => buf.append("\\&")
      case s   => buf.append(s)
    }
    buf.result()
  }

  private def title(
      studyProgram: StudyProgramView,
      semester: Option[Semester]
  )(implicit lang: PrintingLanguage, builder: StringBuilder) =
    builder
      .append("\\title{\n")
      .append(s"\\Huge ${lang.moduleCatalogHeadline} \\\\ [1.5ex]\n")
      .append(
        s"\\LARGE ${escape(studyProgram.localizedLabel(studyProgram.specialization))} PO ${studyProgram.po.version} \\\\ [1ex]\n"
      )
      .append(s"\\LARGE ${studyProgram.degree.localizedDesc} \\\\ [1ex]\n")
      .append(
        semester.fold(lang.previewLabel)(s =>
          s"\\LARGE ${s.localizedLabel} ${s.year}\n"
        )
      )
      .append("}\n")
      .append("\\author{TH Köln, Campus Gummersbach}\n")
      .append("\\date{\\today}\n")

  private def packages(
      draft: Boolean
  )(implicit lang: PrintingLanguage, builder: StringBuilder) =
    builder
      .append("% packages\n")
      .append("\\usepackage[english, ngerman]{babel}\n")
      .append(
        "\\usepackage[a4paper, total={16cm, 24cm}, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}\n"
      )
      .append("\\usepackage{layout}\n")
      .append("\\usepackage{tabularx}\n")
      .append("\\usepackage{hyperref}\n")
      .append("\\usepackage{titlesec}\n")
      .append("\\usepackage{fancyhdr} % customize the page header\n")
      .append("\\usepackage{parskip} % customize paragraph style\n")
      .appendOpt(
        Option.when(draft)(
          s"\\usepackage[colorspec=0.9,text=${lang.previewLabel}]{draftwatermark}\n"
        )
      )

  private def headlineFormats(implicit builder: StringBuilder) =
    builder
      .append("% define the chapter format\n")
      .append("\\titleformat{\\chapter}[display]\n")
      .append("{\\normalfont\\Huge\\bfseries} % font attributes\n")
      .append(
        "{\\vspace*{\\fill}} % vertical space before the chapter title\n"
      )
      .append(
        "{0pt} % horizontal space between the chapter title and the left margin\n"
      )
      .append("{\\Huge\\centering} % font size of the chapter title\n")
      .append(
        "[\\vspace*{\\fill}] % vertical space after the chapter title\n"
      )
      .append("% define the subsection format\n")
      .append("\\titleformat{name=\\subsection}\n")
      .append("{\\normalfont\\large\\bfseries} % default font attributes\n")
      .append("{} % remove numbers\n")
      .append("{0pt} % no space between subsection and left margin\n")
      .append("{} % nothing after subsection\n")

  private def commands(implicit builder: StringBuilder) =
    builder
      .append("% commands and settings\n")
      .append(
        "\\setcounter{tocdepth}{1} % set tocdepth to 1 (includes only chapters and sections)\n"
      )
      .append(
        "\\providecommand{\\tightlist}{\\setlength{\\itemsep}{0pt}\\setlength{\\parskip}{0pt}}\n"
      )
      .append("% customize the page style\n")
      .append("\\pagestyle{fancy}\n")
      .append("\\fancyhf{} % clear header and footer\n")
      .append(
        "\\renewcommand{\\headrulewidth}{0pt} % remove header rule\n"
      )
      .append(
        "\\fancyfoot[C]{\\thepage} % add page number to the center of the footer\n"
      )
      .append(
        "\\setlength{\\parindent}{0pt} % set paragraph indentation to zero\n"
      )
      .append(
        "\\setlength{\\parskip}{0.5\\baselineskip} % set vertical space between paragraphs\n"
      )
      .append("\\setlength{\\marginparwidth}{0pt} % no margin notes\n")
      .append("\\setlength{\\marginparsep}{0pt} % no margin notes\n")

  private def chapter(name: String)(implicit builder: StringBuilder) =
    builder.append(s"\\chapter{$name}\n")

  private def section(text: String)(implicit builder: StringBuilder) =
    builder.append(s"\\section{${escape(text)}}\n")

  private def newPage(implicit builder: StringBuilder) =
    builder.append("\\newpage\n")
}
