package printing.latex

import database._
import models.core._
import models.{Semester, StudyProgramView}
import monocle.Lens
import monocle.macros.GenLens
import ops.StringBuilderOps.SBOps
import parsing.types.{Content, Module}
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
import validator.ModuleRelation

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
      mcs: Seq[Module],
      studyProgramViews: Seq[StudyProgramView]
  )(implicit
      language: PrintingLanguage
  ): StringBuilder = {
    // TODO this is a bad implementation, because it transforms each Module to ModuleOutput
    val entries = List.newBuilder[ModuleOutput]
    val moduleTypes = Set.newBuilder[ModuleType]
    val languages = Set.newBuilder[Language]
    val seasons = Set.newBuilder[Season]
    val people = Set.newBuilder[Identity]
    val assessmentMethods = Set.newBuilder[AssessmentMethod]

    mcs.foreach { mc =>
      moduleTypes += mc.metadata.kind
      languages += mc.metadata.language
      seasons += mc.metadata.season
      people ++= mc.metadata.responsibilities.moduleManagement
      people ++= mc.metadata.responsibilities.lecturers
      assessmentMethods ++= mc.metadata.assessmentMethods.mandatory
        .map(_.method)
      assessmentMethods ++= mc.metadata.assessmentMethods.optional
        .map(_.method)

      entries += ModuleOutput(
        "",
        MetadataOutput(
          mc.metadata.id,
          mc.metadata.title,
          mc.metadata.abbrev,
          mc.metadata.kind.id,
          mc.metadata.ects.value,
          mc.metadata.language.id,
          mc.metadata.duration,
          mc.metadata.season.id,
          mc.metadata.workload,
          mc.metadata.status.id,
          mc.metadata.location.id,
          mc.metadata.participants,
          mc.metadata.relation.map {
            case ModuleRelation.Parent(children) =>
              ModuleRelationOutput.Parent(children.map(_.id))
            case ModuleRelation.Child(parent) =>
              ModuleRelationOutput.Child(parent.id)
          },
          mc.metadata.responsibilities.moduleManagement.map(_.id),
          mc.metadata.responsibilities.lecturers.map(_.id),
          AssessmentMethodsOutput(
            mc.metadata.assessmentMethods.mandatory.map(a =>
              AssessmentMethodEntryOutput(
                a.method.id,
                a.percentage,
                a.precondition.map(_.id)
              )
            ),
            mc.metadata.assessmentMethods.optional.map(a =>
              AssessmentMethodEntryOutput(
                a.method.id,
                a.percentage,
                a.precondition.map(_.id)
              )
            )
          ),
          PrerequisitesOutput(
            mc.metadata.prerequisites.recommended.map(e =>
              PrerequisiteEntryOutput(
                e.text,
                e.modules.map(_.id),
                e.pos.map(_.id)
              )
            ),
            mc.metadata.prerequisites.required.map(e =>
              PrerequisiteEntryOutput(
                e.text,
                e.modules.map(_.id),
                e.pos.map(_.id)
              )
            )
          ),
          POOutput(
            mc.metadata.validPOs.mandatory.map(a =>
              POMandatoryOutput(
                a.po.id,
                a.specialization.map(_.id),
                a.recommendedSemester
              )
            ),
            mc.metadata.validPOs.optional.map(a =>
              POOptionalOutput(
                a.po.id,
                a.specialization.map(_.id),
                a.instanceOf.id,
                a.partOfCatalog,
                a.recommendedSemester
              )
            )
          ),
          mc.metadata.competences.map(_.id),
          mc.metadata.globalCriteria.map(_.id),
          mc.metadata.taughtWith.map(_.id)
        ),
        mc.deContent,
        mc.enContent
      )
    }

    print(
      studyProgram,
      None,
      entries.result(),
      moduleTypes.result().toSeq,
      languages.result().toSeq,
      seasons.result().toSeq,
      people.result().toSeq,
      assessmentMethods.result().toSeq,
      studyProgramViews
    )
  }

  def print(
      studyProgram: StudyProgramView,
      semester: Option[Semester],
      entries: Seq[ModuleOutput],
      moduleTypes: Seq[ModuleType],
      languages: Seq[Language],
      seasons: Seq[Season],
      people: Seq[Identity],
      assessmentMethods: Seq[AssessmentMethod],
      studyProgramViews: Seq[StudyProgramView]
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
    builder.append("\\tableofcontents\n")
    headlineFormats
    chapter(lang.prologHeadline)
    newPage
    chapter(lang.moduleHeadline)
    newPage
    modules(
      studyProgram.poId,
      entries,
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

  private def modules(
      po: String,
      entries: Seq[ModuleOutput],
      moduleTypes: Seq[ModuleType],
      languages: Seq[Language],
      seasons: Seq[Season],
      people: Seq[Identity],
      assessmentMethods: Seq[AssessmentMethod],
      studyProgramViews: Seq[StudyProgramView]
  )(implicit lang: PrintingLanguage, builder: StringBuilder) = {
    def row(key: String, value: String) =
      builder.append(s"$key & $value \\\\\n")

    def content(
        id: UUID,
        deContent: Content,
        enContent: Content,
        entries: List[(String, Lens[Content, String])]
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

    def go(e: ModuleOutput): Unit = {
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
                val studyProgram = studyProgramViews.find(_.poId == p.po).get
                val spLabel = {
                  val spLabel = escape(
                    studyProgram.studyProgram
                      .localizedLabel(studyProgram.specialization)
                  )
                  // TODO Workaround
                  if (studyProgram.poId.endsWith("flex")) s"$spLabel-Flex"
                  else spLabel
                }
                builder
                  .append(
                    s"${studyProgram.degree.localizedLabel}: "
                  )
                  .append(spLabel)
                  .append(s" PO ${studyProgram.poVersion}")
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
          .find(_.id == e.metadata.moduleType)
          .get
          .localizedLabel
      )
      row(lang.ectsLabel, fmtDouble(e.metadata.ects))
      row(
        lang.languageLabel,
        languages
          .find(_.id == e.metadata.language)
          .get
          .localizedLabel
      )
      row(lang.durationLabel, lang.durationValue(e.metadata.duration))
      row(
        lang.frequencyLabel,
        seasons.find(_.id == e.metadata.season).get.localizedLabel
      )
      row(
        lang.moduleCoordinatorLabel,
        fmtCommaSeparated(
          people.filter(p => e.metadata.moduleManagement.contains(p.id)).sorted,
          "\\newline "
        )(fmtIdentity)
      )
      row(
        lang.lecturersLabel,
        fmtCommaSeparated(
          people.filter(p => e.metadata.lecturers.contains(p.id)).sorted,
          "\\newline "
        )(fmtIdentity)
      )
      row(
        lang.assessmentMethodLabel,
        fmtCommaSeparated(
          e.metadata.assessmentMethods.mandatory.sortBy(_.method),
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
        e.metadata.id,
        e.deContent,
        e.enContent,
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
        s"\\LARGE ${escape(studyProgram.studyProgram.localizedLabel(studyProgram.specialization))} PO ${studyProgram.poVersion} \\\\ [1ex]\n"
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
