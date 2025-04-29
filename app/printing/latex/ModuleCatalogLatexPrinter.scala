package printing.latex

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.UUID

import scala.annotation.unused

import catalog.Semester
import models.*
import models.core.*
import monocle.macros.GenLens
import monocle.Lens
import ops.StringBuilderOps.SBOps
import parsing.types.ModuleContent
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.fmtCommaSeparated
import printing.fmtDouble
import printing.fmtIdentity
import printing.pandoc.PandocApi
import printing.IDLabelDescOps
import printing.LabelOps
import printing.LabelOptOps
import printing.LanguageOps
import printing.PrintingLanguage
import service.modulediff.ModuleProtocolDiff

object ModuleCatalogLatexPrinter {
  private def chapter(name: String)(implicit builder: StringBuilder) =
    builder.append(s"\\chapter{$name}\n")

  private def newPage(implicit builder: StringBuilder) =
    builder.append("\\newpage\n")

  private def nameRef(module: UUID) =
    s"\\nameref{sec:${module.toString}}"

  private def prologContent: IntroContent =
    (pLang, _, builder) => {
      chapter(pLang.prologHeadline)(builder)
    }

  private def studyPlanContent: IntroContent =
    (pLang, _, builder) => {
      chapter(pLang.studyPlanHeadline)(builder)
    }

  @unused
  private def layoutContent: IntroContent =
    (_, _, builder) => {
      builder.append("\\layout*\n")
    }

  private def diffContent(diffs: Seq[(ModuleCore, Set[String])], messagesApi: MessagesApi): IntroContent =
    (_, lang, builder) => {
      if (diffs.nonEmpty) {
        val sectionTitle = messagesApi("latex.module_diff.section.title")(lang)
        val sectionIntro = messagesApi("latex.module_diff.section.intro")(lang)
        chapter(sectionTitle)(builder)
        newPage(builder)
        builder.append(s"$sectionIntro\n")
        diffs
          .filter(_._2.nonEmpty)
          .sortBy(_._1.title)
          .foreach {
            case (module, changedKeys) =>
              val ref = nameRef(module.id)
              builder.append(s"\\subsection*{$ref}\n")
              builder.append("\\begin{itemize}\n")
              changedKeys.toList.sorted.foreach { key =>
                val normalizedKey = ModuleKey.normalizeKeyValue(key)
                val label         = messagesApi(normalizedKey + ".label")(lang)
                builder.append(s"\\item $label\n")
              }
              builder.append("\\end{itemize}\n")
          }
      }
    }

  // TODO This filter should should become obsolete soon. The assertion function below ensures the invariance of valid module-po relationships
  private def validModulesForStudyProgram(
      modules: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])],
      sp: StudyProgramView
  ) =
    modules.filter {
      case (m, _) =>
        val isValid = m.metadata.po.mandatory.exists { a =>
          a.po == sp.po.id && a.specialization
            .zip(sp.specialization)
            .fold(true)(a => a._1 == a._2.id)
        }
        assert(
          isValid,
          s"module ${m.id.getOrElse(m.metadata.title)} should not be in the selection for module catalog of ${sp.fullPoId.id}, because ${m.metadata.po.mandatory.map(_.fullPo)}"
        )
        ModuleStatus.isActive(m.metadata.status) && isValid
    }

  def preview(
      pandocApi: PandocApi,
      messagesApi: MessagesApi,
      diffs: Seq[(ModuleCore, Set[String])],
      introContent: Option[IntroContent],
      modules: Seq[(ModuleProtocol, Option[LocalDateTime])],
      payload: Payload,
      pLang: PrintingLanguage,
      lang: Lang,
  ) = {
    val diffContent = this.diffContent(diffs, messagesApi)
    val intro       = introContent.fold(List(diffContent))(List(diffContent, _))
    new ModuleCatalogLatexPrinter(
      pandocApi,
      messagesApi,
      None,
      validModulesForStudyProgram(modules, payload.studyProgram),
      payload,
      intro,
      diffs
    )(
      using pLang,
      lang
    )
  }

  def default(
      pandocApi: PandocApi,
      messagesApi: MessagesApi,
      semester: Semester,
      modules: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])],
      payload: Payload,
      pLang: PrintingLanguage,
      lang: Lang
  ) =
    new ModuleCatalogLatexPrinter(
      pandocApi,
      messagesApi,
      Some(semester),
      validModulesForStudyProgram(modules, payload.studyProgram),
      payload,
      List(prologContent, studyPlanContent),
      Nil
    )(using pLang, lang)
}

/**
 * Style from: https://www.overleaf.com/learn/latex/Page_size_and_margins
 */
final class ModuleCatalogLatexPrinter(
    pandocApi: PandocApi,
    messagesApi: MessagesApi,
    semester: Option[Semester],
    modulesInPO: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])],
    payload: Payload,
    introContent: List[IntroContent],
    diffs: Seq[(ModuleCore, Set[String])]
)(
    using pLang: PrintingLanguage,
    lang: Lang
) extends Logging {

  // TODO: replace PrintingLanguage with Lang
  import ModuleCatalogLatexPrinter.chapter
  import ModuleCatalogLatexPrinter.nameRef
  import ModuleCatalogLatexPrinter.newPage

  private given builder: StringBuilder = new StringBuilder()

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy", lang.locale)

  private given Ordering[Identity] =
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

  def print(): StringBuilder = {
    builder.append("\\documentclass[article, 11pt, oneside]{book}\n")
    packages(semester.isEmpty)
    commands()
    builder.append("\\begin{document}\n")
    builder.append(s"\\selectlanguage{${pLang.fold("ngerman", "english")}}\n")
    title()
    builder.append("\\maketitle\n")
    newPage
    builder.append("\\tableofcontents\n")
    newPage
    headlineFormats
    introContent.foreach { c =>
      c.printWithNewPage(pLang, lang, builder)
      newPage
    }
    chapter(pLang.moduleHeadline)
    newPage
    modules()
    builder.append("\\end{document}")
  }

  private def modules() = {
    if (modulesInPO.isEmpty) newPage
    else
      modulesInPO
        .sortBy(_._1.metadata.title)
        .foreach {
          case (m, lm) =>
            m.metadata.moduleRelation match {
              case Some(ModuleRelationProtocol.Child(_)) =>
              case Some(ModuleRelationProtocol.Parent(children)) =>
                module(m, lm, isChild = false)
                newPage
                children.toList
                  .map { id =>
                    val module = modulesInPO.find(_._1.id.contains(id))
                    if module.isEmpty then logger.error(s"unable to find child module $id from parent ${m.id}")
                    module
                  }
                  .collect { case Some(m) => m }
                  .sortBy(_._1.metadata.title)
                  .foreach {
                    case (m, lm) =>
                      module(m, lm, isChild = true)
                      newPage
                  }
              case None =>
                module(m, lm, isChild = false)
                newPage
            }
        }
  }

  private def title() =
    builder
      .append("\\title{\n")
      .append(s"\\Huge ${pLang.moduleCatalogHeadline} \\\\ [1.5ex]\n")
      .append(
        s"\\LARGE ${escape(payload.studyProgram.localizedLabel(payload.studyProgram.specialization))} PO ${payload.studyProgram.po.version} \\\\ [1ex]\n"
      )
      .append(s"\\LARGE ${payload.studyProgram.degree.localizedDesc} \\\\ [1ex]\n")
      .append(semester.fold(pLang.previewLabel)(s => s"\\LARGE ${s.localizedLabel} ${s.year}\n"))
      .append("}\n")
      .append("\\author{TH Köln, Campus Gummersbach}\n")
      .append("\\date{\\today}\n")

  private def packages(isDraft: Boolean) =
    builder
      .append("% packages\n")
      .append("\\usepackage[english, ngerman]{babel}\n")
      .append(
        "\\usepackage[a4paper, total={16cm, 24cm}, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}\n"
      )
      .append("\\usepackage{layout}\n")
      .append("\\usepackage{tabularx}\n")
      .append("\\usepackage{hyperref} % support for hyperlinks\n")
      .append("\\usepackage{xurl} % line breaking in urls\n")
      .append("\\usepackage{titlesec}\n")
      .append("\\usepackage{fancyhdr} % customize the page header\n")
      .append("\\usepackage{parskip} % customize paragraph style\n")
      .appendOpt(
        Option.when(isDraft)(
          s"""\\usepackage[colorspec=0.9,text=${pLang.previewLabel}]{draftwatermark} % watermark
             |\\usepackage[defaultcolor=orange]{changes} % highlights changes (https://ctan.org/pkg/changes?lang=en)\n""".stripMargin
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
//      .append("% define the subsection format\n")
//      .append("\\titleformat{name=\\subsection}\n")
//      .append("{\\normalfont\\large\\bfseries} % default font attributes\n")
//      .append("{} % remove numbers\n")
//      .append("{0pt} % no space between subsection and left margin\n")
//      .append("{} % nothing after subsection\n")

  private def commands() =
    builder
      .append("% commands and settings\n")
//      .append("\\setcounter{tocdepth}{1} % set tocdepth to 1 (includes only chapters and sections)\n")
      .append(
        "\\setcounter{tocdepth}{2} % set tocdepth to 2 (includes chapters, sections (modules) and subsections (child modules))\n"
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

  private def rewriteSubsections(origin: String) = {
    val pattern = """\\subsection\{([^}]*)\}""".r
    val result  = new StringBuilder()
    var lastEnd = 0

    for (m <- pattern.findAllMatchIn(origin)) {
      val subsectionName = m.group(1)
      result.append(origin.substring(lastEnd, m.start))
      result.append(s"\\subsection*{$subsectionName}")
      lastEnd = m.end
    }

    result.append(origin.substring(lastEnd))
    result.result()
  }

  private def replaceSubsection(origin: String, p: String => Boolean, replacement: String => String) = {
    val pattern = """\\subsection*\{([^}]*)\}""".r
    val result  = new StringBuilder()
    var lastEnd = 0

    for (m <- pattern.findAllMatchIn(origin)) {
      val subsectionName = m.group(1)
      result.append(origin.substring(lastEnd, m.start))

      if p(subsectionName) then result.append(s"\\subsection*{${replacement(subsectionName)}}")
      else result.append(m.matched)

      lastEnd = m.end
    }

    result.append(origin.substring(lastEnd))
    result
  }

  private def module(
      module: ModuleProtocol,
      lastModified: LocalDateTime | Option[LocalDateTime],
      isChild: Boolean
  ): Unit = {
    def row(key: String, value: String) =
      builder.append(s"$key & $value \\\\\n")

    val po                = payload.studyProgram.po.id
    val moduleTypes       = payload.moduleTypes
    val languages         = payload.languages
    val seasons           = payload.seasons
    val people            = payload.people
    val assessmentMethods = payload.assessmentMethods
    val studyProgramViews = payload.studyProgramViews

    val diffs = this.diffs.find(_._1.id == module.id.get).map(_._2)

    def highlightIf(str: String, p: String => Boolean) =
      if diffs.exists(_.exists(p)) then highlight(str) else str

    val (workload, contactHour, selfStudy) =
      pLang.workload(module.metadata.workload)

    def poMandatoryRow =
      if (module.metadata.po.mandatory.size == 1) pLang.noneLabel
      else
        module.metadata.po.mandatory
          .sortBy(_.po)
          .collect {
            case p if p.po != po && studyProgramViews.exists(_.po.id == p.po) =>
              val builder      = new StringBuilder()
              val studyProgram = studyProgramViews.find(_.po.id == p.po).get
              val spLabel      = escape(studyProgram.localizedLabel(studyProgram.specialization))
              builder
                .append(s"${studyProgram.degree.localizedLabel}: ")
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

    def prerequisitesLabelRow(p: Option[ModulePrerequisiteEntryProtocol]) =
      p match
        case Some(p) =>
          val builder = new StringBuilder()
          if p.text.nonEmpty then {
            builder.append(p.text)
          }
          if p.modules.nonEmpty then {
            val subBuilder  = new StringBuilder()
            val moduleLabel = messagesApi("latex.module.label")
            subBuilder.append(s"$moduleLabel: ")
            p.modules.zipWithIndex.foreach {
              case (m, i) =>
                val module = payload.modules.find(_.id == m).get
                if modulesInPO.exists(_._1.id.get == m) then subBuilder.append(nameRef(m))
                else subBuilder.append(s"${module.title} (${module.abbrev})")
                if i < p.modules.size - 1 then subBuilder.append(", ")
            }
            if builder.nonEmpty then {
              builder.append("\\newline ")
            }
            builder.append(subBuilder.toString())
          }
          builder.toString()
        case None =>
          pLang.noneLabel

    def assessmentMethodsRow =
      if module.metadata.assessmentMethods.mandatory.isEmpty then pLang.noneLabel
      else
        fmtCommaSeparated(module.metadata.assessmentMethods.mandatory.sortBy(_.method), "\\newline ") { a =>
          val method = assessmentMethods.find(_.id == a.method).localizedLabel
          a.percentage.fold(method)(d => s"$method (${fmtDouble(d)} \\%)")
        }

    def lastModifiedRow =
      lastModified match
        case lm: LocalDateTime => lm.format(localDatePattern)
        case Some(lm)          => lm.format(localDatePattern)
        case None              => pLang.unknownLabel

    sectionWithRef(module.metadata.title, module.id, isChild)
    builder.append("\\begin{tabularx}{\\linewidth}{@{}>{\\bfseries}l@{\\hspace{.5em}}X@{}}\n")
    row("ID", module.id.fold("Unknown ID")(_.toString))
    row(pLang.lastModifiedLabel, lastModifiedRow)
    row(highlightIf(pLang.moduleCodeLabel, ModuleProtocolDiff.isModuleAbbrev), escape(module.metadata.abbrev))
    row(highlightIf(pLang.moduleTitleLabel, ModuleProtocolDiff.isModuleTitle), escape(module.metadata.title))
    row(
      highlightIf(pLang.moduleTypeLabel, ModuleProtocolDiff.isModuleModuleType),
      moduleTypes.find(_.id == module.metadata.moduleType).localizedLabel,
    )
    row(highlightIf(pLang.ectsLabel, ModuleProtocolDiff.isModuleEcts), fmtDouble(module.metadata.ects))
    row(
      highlightIf(pLang.languageLabel, ModuleProtocolDiff.isModuleLanguage),
      languages.find(_.id == module.metadata.language).localizedLabel
    )
    row(
      highlightIf(pLang.durationLabel, ModuleProtocolDiff.isModuleDuration),
      pLang.durationValue(module.metadata.duration)
    )
    row(
      highlightIf(pLang.frequencyLabel, ModuleProtocolDiff.isModuleSeason),
      seasons.find(_.id == module.metadata.season).localizedLabel
    )
    row(
      highlightIf(pLang.moduleCoordinatorLabel, ModuleProtocolDiff.isModuleModuleManagement),
      fmtCommaSeparated(
        people.filter(p => module.metadata.moduleManagement.exists(_ == p.id)).sorted,
        "\\newline "
      )(fmtIdentity)
    )
    row(
      highlightIf(pLang.lecturersLabel, ModuleProtocolDiff.isModuleLecturers),
      fmtCommaSeparated(people.filter(p => module.metadata.lecturers.exists(_ == p.id)).sorted, "\\newline ")(
        fmtIdentity
      )
    )
    row(
      highlightIf(pLang.assessmentMethodLabel, ModuleProtocolDiff.isModuleAssessmentMethodsMandatory),
      assessmentMethodsRow
    )
    row(highlightIf(workload._1, ModuleProtocolDiff.isModuleWorkload), workload._2)
    row(highlightIf(contactHour._1, ModuleProtocolDiff.isModuleWorkload), contactHour._2)
    row(highlightIf(selfStudy._1, ModuleProtocolDiff.isModuleWorkload), selfStudy._2)
    row(
      highlightIf(pLang.recommendedPrerequisitesLabel, ModuleProtocolDiff.isModuleRecommendedPrerequisites),
      prerequisitesLabelRow(module.metadata.prerequisites.recommended)
    )
    row(
      highlightIf(pLang.requiredPrerequisitesLabel, ModuleProtocolDiff.isModuleRequiredPrerequisites),
      prerequisitesLabelRow(module.metadata.prerequisites.required)
    )
    row(highlightIf(pLang.poLabelShort, ModuleProtocolDiff.isPOMandatory), poMandatoryRow)
    builder.append("\\end{tabularx}\n")
    moduleContent(
      module.id,
      module.deContent,
      module.enContent,
      List(
        (pLang.learningOutcomeLabel, GenLens[ModuleContent](_.learningOutcome)),
        (pLang.moduleContentLabel, GenLens[ModuleContent](_.content)),
        (pLang.teachingAndLearningMethodsLabel, GenLens[ModuleContent](_.teachingAndLearningMethods)),
        (pLang.recommendedReadingLabel, GenLens[ModuleContent](_.recommendedReading)),
        (pLang.particularitiesLabel, GenLens[ModuleContent](_.particularities))
      ),
      diffs
    )
  }

  private def moduleContent(
      id: Option[UUID],
      deContent: ModuleContent,
      enContent: ModuleContent,
      entries: List[(String, Lens[ModuleContent, String])],
      diffs: Option[Set[String]]
  ): Unit = {
    val markdownContent = new StringBuilder()
    entries.foreach {
      case (headline, lens) =>
        val content = pLang.fold(lens.get(deContent), lens.get(enContent))
        if content.nonEmpty && !content.forall(_.isWhitespace) then {
          markdownContent.append(s"## $headline\n")
          markdownContent.append(content)
          markdownContent.append("\n\n")
        }
    }
    pandocApi.toLatex(markdownContent.toString()).map(rewriteSubsections) match {
      case Left((e, stdErr)) =>
        logger.error(
          s"""content conversation from markdown to latex failed on $id:
             |  - throwable: ${e.getMessage}
             |  - sdtErr: $stdErr""".stripMargin
        )
        builder.append("ERROR\n\n")
      case Right(text) =>
        diffs match
          case Some(diffs) =>
            val contentDiffs = diffs.collect { case d if ModuleProtocolDiff.isModuleContent(d) => d.split('.').last }
            if contentDiffs.isEmpty then {
              builder.append(text)
            } else {
              val replacedWithDiffs = replaceSubsection(
                text,
                subsection => {
                  val key =
                    if subsection == pLang.learningOutcomeLabel then ModuleProtocolDiff.learningOutcomeKey
                    else if subsection == pLang.moduleContentLabel then ModuleProtocolDiff.moduleContentKey
                    else if subsection == pLang.teachingAndLearningMethodsLabel then
                      ModuleProtocolDiff.teachingAndLearningMethodsKey
                    else if subsection == pLang.recommendedReadingLabel then ModuleProtocolDiff.recommendedReadingKey
                    else if subsection == pLang.particularitiesLabel then ModuleProtocolDiff.particularitiesKey
                    else ""
                  contentDiffs.contains(key)
                },
                old => highlight(old)
              )
              builder.append(replacedWithDiffs)
            }
          case None =>
            builder.append(text)
    }
  }

  private def sectionWithRef(text: String, id: Option[UUID], isChild: Boolean)(implicit builder: StringBuilder) = {
    val title = escape(text)
    id match
      case Some(id) =>
        val ref = s"\\label{sec:${id.toString}}"
        if isChild then builder.append(s"\\subsection{$title}$ref\n") else builder.append(s"\\section{$title}$ref\n")
      case None =>
        if isChild then builder.append(s"\\subsection{$title}\n") else builder.append(s"\\section{$title}\n")
  }

  private def highlight(str: String) =
    s"\\highlight{$str}"
}
