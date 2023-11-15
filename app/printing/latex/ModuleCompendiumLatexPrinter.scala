package printing.latex

import database.ModuleCompendiumOutput
import database.view.SpecializationShort
import models.core._
import models.{POShort, Semester, StudyProgramShort}
import monocle.Lens
import monocle.macros.GenLens
import ops.Measure
import parsing.types.Content
import play.api.Logging
import printer.Printer.{always, newline, prefix}
import printer.{Printer, PrintingError}
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
    extends Logging
    with Measure {

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
  )(implicit pLang: PrintingLanguage): Either[PrintingError, String] =
    measure(f = {
      document(
        title(po.studyProgram, po.specialization, po.version, semester),
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
      ).print((), new StringBuilder()).map(_.toString())
    })

  private def document(
      title: String,
      modules: Printer[Unit]
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    prefix(raw"\documentclass[article, 11pt, oneside]{book}")
      .skip(prefixNewLine(packages()))
      .skip(prefixNewLine(commands()))
      .skip(prefixNewLine(raw"\begin{document}"))
      .skip(setLanguage)
      .skip(prefixNewLine(title))
      .skip(prefixNewLine(raw"\maketitle"))
      .skip(newPage())
      .skip(prefixNewLine("\\layout*"))
      .skip(newPage())
      .skip(prefixNewLine(raw"\tableofcontents"))
      .skip(prefixNewLine(chapterFormat()))
      .skip(chapter(lang.prologHeadline))
      .skip(newPage())
      .skip(chapter(lang.moduleHeadline))
      .skip(prefixNewLine(modules))
      .skip(chapter(lang.studyPlanHeadline))
      .skip(prefixNewLine(raw"\end{document}"))

  private def modules(
      po: String,
      entries: Seq[ModuleCompendiumOutput],
      moduleTypes: Seq[ModuleType],
      languages: Seq[Language],
      seasons: Seq[Season],
      people: Seq[Person],
      assessmentMethods: Seq[AssessmentMethod],
      poShorts: Seq[POShort]
  )(implicit lang: PrintingLanguage): Printer[Unit] = {
    def row(key: String, value: String) =
      prefixNewLine(raw"$key & $value \\")

    def table(rows: List[Printer[Unit]]): Printer[Unit] =
      if (rows.isEmpty) always()
      else
        prefixNewLine(
          raw"\begin{tabularx}{\linewidth}{@{}>{\bfseries}l@{\hspace{.5em}}X@{}}"
        )
          .skip(rows.reduce(_ skip _))
          .skip(prefixNewLine(raw"\end{tabularx}"))

    def content(
        e: ModuleCompendiumOutput,
        entries: List[(String, Lens[Content, String])]
    ): Printer[Unit] = {
      def go(headline: String, lens: Lens[Content, String]): Printer[Unit] = {
        val content = lang.fold(lens.get(e.deContent), lens.get(e.enContent))
        val (texContent, sdtOut, sdtErr) = pandocApi.toLatex(content)
        if (sdtOut.nonEmpty)
          logger.info(
            sdtOut.mkString("\n========\n\t- ", "\n\t- ", "========")
          )
        if (sdtErr.nonEmpty)
          logger.error(
            sdtErr.mkString("\n========\n\t- ", "\n\t- ", "========")
          )
        prefixNewLine(raw"\subsection*{$headline}")
          .skip(lineIfNonEmpty(texContent.fold[String](_.getMessage, identity)))
      }

      entries.foldLeft(always[Unit]()) { case (acc, c) =>
        acc.skip(go(c._1, c._2))
      }
    }

    def go(e: ModuleCompendiumOutput): Printer[Unit] = {
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
        .skip(
          table(
            List(
              row("ID", e.metadata.id.toString),
              row(lang.moduleCodeLabel, escape(e.metadata.abbrev)),
              row(lang.moduleTitleLabel, escape(e.metadata.title)),
              row(
                lang.moduleTypeLabel,
                moduleTypes
                  .find(_.abbrev == e.metadata.moduleType)
                  .get
                  .localizedLabel
              ),
              row(lang.ectsLabel, fmtDouble(e.metadata.ects)),
              row(
                lang.languageLabel,
                languages
                  .find(_.abbrev == e.metadata.language)
                  .get
                  .localizedLabel
              ),
              row(lang.durationLabel, lang.durationValue(e.metadata.duration)),
              row(
                lang.frequencyLabel,
                seasons.find(_.abbrev == e.metadata.season).get.localizedLabel
              ),
              row(
                lang.moduleCoordinatorLabel,
                fmtCommaSeparated(
                  people.filter(p =>
                    e.metadata.moduleManagement.contains(p.id)
                  ),
                  "\\newline "
                )(fmtPerson)
              ),
              row(
                lang.lecturersLabel,
                fmtCommaSeparated(
                  people.filter(p => e.metadata.lecturers.contains(p.id)),
                  "\\newline "
                )(fmtPerson)
              ),
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
                  a.percentage.fold(method)(d =>
                    s"$method (${fmtDouble(d)} \\%)"
                  )
                }
              ),
              row(workload._1, workload._2),
              row(contactHour._1, contactHour._2),
              row(selfStudy._1, selfStudy._2),
              row(
                lang.poLabelShort,
                poMandatory
              )
            )
          )
        )
        .skip(
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
        )
    }

    entries.foldLeft(always[Unit]()) { case (acc, e) =>
      acc.skip(newPage()).skip(prefixNewLine(go(e)))
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
  )(implicit lang: PrintingLanguage): String =
    raw"""\title{
         |\Huge ${lang.moduleCompendiumHeadline} \\ [1.5ex]
         |\LARGE ${escape(sp.localizedLabel(specialization))} PO $po \\ [1ex]
         |\LARGE ${sp.grade.localizedDesc} \\ [1ex]
         |\LARGE ${semester.localizedLabel} ${semester.year}
         |}
         |\author{TH Köln, Campus Gummersbach}
         |\date{\today}""".stripMargin

  private def packages(): String = {
    val builder = new StringBuilder()
    builder.append("% packages\n")
    builder.append("\\usepackage[english, german]{babel}\n")
    builder.append(
      "\\usepackage[a4paper, total={16cm, 24cm}, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}\n"
    )
    builder.append("\\usepackage{layout}\n")
    builder.append("\\usepackage{tabularx}\n")
    builder.append("\\usepackage{hyperref}\n")
    builder.append("\\usepackage{titlesec}\n")
    builder.append("\\usepackage{fancyhdr} % customize the page header\n")
    builder.append("\\usepackage{parskip} % customize paragraph style")
    builder.toString()
  }

  private def chapterFormat(): String =
    raw"""% define the chapter format
           |\titleformat{\chapter}[display]
           |  {\normalfont\Huge\bfseries} % font attributes
           |  {\vspace*{\fill}} % vertical space before the chapter title
           |  {0pt} % horizontal space between the chapter title and the left margin
           |  {\Huge\centering} % font size of the chapter title
           |  [\vspace*{\fill}] % vertical space after the chapter title""".stripMargin

  private def commands(): String =
    raw"""% commands and settings
           |\providecommand{\tightlist}{\setlength{\itemsep}{0pt}\setlength{\parskip}{0pt}}
           |% customize the page style
           |\pagestyle{fancy}
           |\fancyhf{} % clear header and footer
           |\renewcommand{\headrulewidth}{0pt} % remove header rule
           |\fancyfoot[C]{\thepage} % add page number to the center of the footer
           |\setlength{\parindent}{0pt} % set paragraph indentation to zero
           |\setlength{\parskip}{0.5\baselineskip} % set vertical space between paragraphs
           |\setlength{\marginparwidth}{0pt} % no margin notes
           |\setlength{\marginparsep}{0pt} % no margin notes""".stripMargin

  private def chapter(name: String): Printer[Unit] =
    prefixNewLine(raw"\chapter{$name}")

  private def section(text: String): Printer[Unit] =
    prefix(raw"\section{${escape(text)}}")

  private def newPage(): Printer[Unit] =
    prefixNewLine(raw"\newpage")

  private def lineIfNonEmpty(
      text: String
  )(implicit lang: PrintingLanguage): Printer[Unit] =
    if (text.nonEmpty && !text.forall(_.isWhitespace))
      prefixNewLine(prefix(text))
    else prefix(lang.noneLabel)

  private def prefixNewLine(text: String): Printer[Unit] =
    prefixNewLine(prefix(text))

  private def prefixNewLine(printer: Printer[Unit]): Printer[Unit] =
    newline.skip(printer)

  private def setLanguage(implicit lang: PrintingLanguage): Printer[Unit] =
    prefixNewLine(raw"\selectlanguage{${lang.fold("german", "english")}}")
}
