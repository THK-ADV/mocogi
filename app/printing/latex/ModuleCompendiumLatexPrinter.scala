package printing.latex

import database.ModuleCompendiumOutput
import models.core._
import models.{POShort, Semester, SpecializationShort, StudyProgramShort}
import monocle.Lens
import monocle.macros.GenLens
import parsing.types.Content
import play.api.Logging
import printing.pandoc.PandocApi
import printing.{
  AbbrevLabelDescLikeOps,
  AbbrevLabelLikeOps,
  PrintingLanguage,
  fmtCommaSeparated,
  fmtDouble,
  fmtPerson
}

import javax.inject.{Inject, Singleton}

// TODO change implementation to StringBuilder or writing file directly

/** Style from: https://www.overleaf.com/learn/latex/Page_size_and_margins
  */
@Singleton
final class ModuleCompendiumLatexPrinter @Inject() (pandocApi: PandocApi)
    extends Logging {

  def print(
      po: POShort,
      semester: Semester,
      entries: Seq[ModuleCompendiumOutput],
      moduleTypes: Seq[ModuleType],
      languages: Seq[Language],
      seasons: Seq[Season],
      people: Seq[Person],
      assessmentMethods: Seq[AssessmentMethod],
      poShorts: Seq[POShort]
  )(implicit lang: PrintingLanguage): StringBuilder = {
    implicit val builder: StringBuilder = new StringBuilder()
    builder.append("\\documentclass[article, 11pt, oneside]{book}\n")
    packages
    commands
    builder.append("\\begin{document}\n")
    builder.append(s"\\selectlanguage{${lang.fold("german", "english")}}\n")
    title(po.studyProgram, po.specialization, po.version, semester)
    builder.append("\\maketitle\n")
    newPage
    builder.append("\\layout*\n")
    newPage
    builder.append("\\tableofcontents\n")
    headlineFormats
    chapter(lang.prologHeadline)
    newPage
    chapter(lang.moduleHeadline)
    newPage
    modules(
      po.abbrev,
      entries,
      moduleTypes,
      languages,
      seasons,
      people,
      assessmentMethods,
      poShorts
    )
    chapter(lang.studyPlanHeadline)
    builder.append("\\end{document}")
  }

  private def modules(
      po: String,
      entries: Seq[ModuleCompendiumOutput],
      moduleTypes: Seq[ModuleType],
      languages: Seq[Language],
      seasons: Seq[Season],
      people: Seq[Person],
      assessmentMethods: Seq[AssessmentMethod],
      poShorts: Seq[POShort]
  )(implicit lang: PrintingLanguage, builder: StringBuilder) = {
    def row(key: String, value: String) =
      builder.append(s"$key & $value \\\\\n")

    def content(
        mc: ModuleCompendiumOutput,
        entries: List[(String, Lens[Content, String])]
    ): Unit = {
      val markdownContent = new StringBuilder()
      entries.foreach { case (headline, lens) =>
        markdownContent.append(s"## $headline\n")
        val content = lang.fold(lens.get(mc.deContent), lens.get(mc.enContent))
        if (content.nonEmpty && !content.forall(_.isWhitespace))
          markdownContent.append(content)
        else markdownContent.append(lang.noneLabel)
        markdownContent.append("\n\n")
      }
      pandocApi.toLatex(markdownContent.toString()) match {
        case Left((e, stdErr)) =>
          logger.error(
            s"""content conversation from markdown to latex failed on ${mc.metadata.id}:
               |  - throwable: ${e.getMessage}
               |  - sdtErr: $stdErr""".stripMargin
          )
          builder.append("ERROR\n\n")
        case Right(text) => builder.append(text)
      }
    }

    def go(e: ModuleCompendiumOutput): Unit = {
      val (workload, contactHour, selfStudy) =
        lang.workload(e.metadata.workload)
      val poMandatory =
        if (e.metadata.po.mandatory.size == 1) lang.noneLabel
        else
          e.metadata.po.mandatory
            .sortBy(_.po)
            .collect {
              case p if p.po != po =>
                val builder = new StringBuilder()
                val poShort = poShorts.find(_.abbrev == p.po).get
                val spLabel = {
                  val spLabel = escape(
                    poShort.studyProgram
                      .localizedLabel(poShort.specialization)
                  )
                  // TODO Workaround
                  if (poShort.abbrev.endsWith("flex")) s"$spLabel-Flex"
                  else spLabel
                }
                builder
                  .append(
                    s"${poShort.studyProgram.grade.localizedLabel}: "
                  )
                  .append(spLabel)
                  .append(s" PO ${poShort.version}")
                if (p.recommendedSemester.nonEmpty) {
                  builder.append(
                    s" (Semester ${fmtCommaSeparated(p.recommendedSemester)(_.toString())})"
                  )
                }
                builder.toString()
            }
            .mkString("\\newline ")

      section(e.metadata.title)
      builder.append(
        "\\begin{tabularx}{\\linewidth}{@{}>{\\bfseries}l@{\\hspace{.5em}}X@{}}\n"
      )
      row("ID", e.metadata.id.toString)
      row(lang.moduleCodeLabel, escape(e.metadata.abbrev))
      row(lang.moduleTitleLabel, escape(e.metadata.title))
      row(
        lang.moduleTypeLabel,
        moduleTypes
          .find(_.abbrev == e.metadata.moduleType)
          .get
          .localizedLabel
      )
      row(lang.ectsLabel, fmtDouble(e.metadata.ects))
      row(
        lang.languageLabel,
        languages
          .find(_.abbrev == e.metadata.language)
          .get
          .localizedLabel
      )
      row(lang.durationLabel, lang.durationValue(e.metadata.duration))
      row(
        lang.frequencyLabel,
        seasons.find(_.abbrev == e.metadata.season).get.localizedLabel
      )
      row(
        lang.moduleCoordinatorLabel,
        fmtCommaSeparated(
          people.filter(p => e.metadata.moduleManagement.contains(p.id)),
          "\\newline "
        )(fmtPerson)
      )
      row(
        lang.lecturersLabel,
        fmtCommaSeparated(
          people.filter(p => e.metadata.lecturers.contains(p.id)),
          "\\newline "
        )(fmtPerson)
      )
      row(
        lang.assessmentMethodLabel,
        fmtCommaSeparated(
          e.metadata.assessmentMethods.mandatory,
          "\\newline "
        ) { a =>
          val method = assessmentMethods
            .find(_.abbrev == a.method)
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
        e,
        List(
          (lang.learningOutcomeLabel, GenLens[Content](_.learningOutcome)),
          (lang.moduleContentLabel, GenLens[Content](_.content)),
          (
            lang.teachingAndLearningMethodsLabel,
            GenLens[Content](_.teachingAndLearningMethods)
          ),
          (
            lang.recommendedReadingLabel,
            GenLens[Content](_.recommendedReading)
          ),
          (lang.particularitiesLabel, GenLens[Content](_.particularities))
        )
      )
    }

    if (entries.isEmpty) newPage
    else
      entries.foreach { e =>
        go(e)
        newPage
      }
  }

  // TODO find a better solution for this
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
      sp: StudyProgramShort,
      specialization: Option[SpecializationShort],
      po: Int,
      semester: Semester
  )(implicit lang: PrintingLanguage, builder: StringBuilder) =
    builder
      .append("\\title{\n")
      .append(s"\\Huge ${lang.moduleCompendiumHeadline} \\\\ [1.5ex]\n")
      .append(
        s"\\LARGE ${escape(sp.localizedLabel(specialization))} PO $po \\\\ [1ex]\n"
      )
      .append(s"\\LARGE ${sp.grade.localizedDesc} \\\\ [1ex]\n")
      .append(s"\\LARGE ${semester.localizedLabel} ${semester.year}\n")
      .append("}\n")
      .append("\\author{TH Köln, Campus Gummersbach}\n")
      .append("\\date{\\today}\n")

  private def packages(implicit builder: StringBuilder) =
    builder
      .append("% packages\n")
      .append("\\usepackage[english, german]{babel}\n")
      .append(
        "\\usepackage[a4paper, total={16cm, 24cm}, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}\n"
      )
      .append("\\usepackage{layout}\n")
      .append("\\usepackage{tabularx}\n")
      .append("\\usepackage{hyperref}\n")
      .append("\\usepackage{titlesec}\n")
      .append("\\usepackage{fancyhdr} % customize the page header\n")
      .append("\\usepackage{parskip} % customize paragraph style\n")

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
