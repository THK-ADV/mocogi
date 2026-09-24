package printing.latex

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import scala.collection.mutable

import models.*
import models.core.*
import monocle.macros.GenLens
import monocle.Lens
import ops.appendOpt
import parsing.types.ModuleContent
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.fmtCommaSeparated
import printing.fmtDouble
import printing.fmtIdentity
import printing.latex.snippet.LatexContentSnippet
import printing.LocalizedStrings

private enum RenderingContext {
  case Mandatory
  case Elective
  case FieldOfStudy(po: String)
  case None
}

object ModuleCatalogLatexPrinter {
  def section(name: String)(implicit builder: StringBuilder) =
    builder.append(s"\\section{$name}\n")

  def newPage(implicit builder: StringBuilder) =
    builder.append("\\newpage\n")

  def nameRef(module: UUID) =
    s"\\nameref{sec:${module.toString}}"
}

/**
 * Style from: https://www.overleaf.com/learn/latex/Page_size_and_margins
 */
final class ModuleCatalogLatexPrinter(
    printer: MarkdownLatexPrinter,
    messages: MessagesApi,
    semester: Option[Semester],
    pos: Seq[StudyProgramView],
    currentPO: PO,
    modulesInPO: Vector[(ModuleProtocol, LocalDate)],
    children: Vector[(ModuleProtocol, LocalDate)],
    payload: Payload,
    latexSnippets: List[LatexContentSnippet],
    postTitleSnippets: List[LatexContentSnippet]
)(using lang: Lang)
    extends Logging {

  import ModuleCatalogLatexPrinter.nameRef
  import ModuleCatalogLatexPrinter.newPage
  import ModuleCatalogLatexPrinter.section

  private given builder: StringBuilder = new StringBuilder()

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy", lang.locale)

  private val consumedModules = mutable.HashSet.empty[UUID]

  private var renderingContext = RenderingContext.None

  /** Every module printed by this catalog: the modules of the PO plus the children rendered inside them. */
  private val printedModules = modulesInPO ++ children

  private val printedModulesById = printedModules.flatMap { module =>
    module._1.id.map(_ -> module)
  }.toMap

  private def isPreview = semester.isEmpty

  private val strings = new LocalizedStrings(messages)

  private def currentModuleType(m: MetadataProtocol, parent: Option[UUID]) =
    renderingContext match {
      case RenderingContext.Mandatory | RenderingContext.FieldOfStudy(_) =>
        parent match {
          case Some(parent) => s"Teilmodul von ${nameRef(parent)}"
          case None         =>
            m.moduleRelation match {
              case Some(relation) =>
                val submodules = relation.children.map(nameRef).toList.mkString(", ")
                s"Obermodul von $submodules"
              case None => "Pflichtmodul"
            }
        }
      case RenderingContext.Elective =>
        val baseStr        = "Wahlmodul"
        val genericModules = m.po.optional
          .filter(_.po == currentPO.id)
          .map(_.instanceOf)
          .distinct
          .map { id =>
            if printedModulesById.contains(id) then nameRef(id)              // show generic module ref
            else payload.modules.find(_.id == id).map(_.title).getOrElse("") // show module title
          }
          .filter(_.nonEmpty)

        if genericModules.isEmpty then baseStr
        else s"$baseStr (${genericModules.mkString(", ")})"
      case RenderingContext.None => "Keine Angabe"
    }

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
    builder.append("\\documentclass[11pt, oneside]{article}")
    packages()
    commands()
    headlineFormats
    builder.append(s"""
                      |\\begin{document}
                      |\\selectlanguage{${strings.languagePackage}}""".stripMargin)
    title()
    if postTitleSnippets.nonEmpty then {
      newPage
      postTitleSnippets.foreach(_.print(using lang, builder))
    }
    newPage
    builder.append("\\tableofcontents\n")
    newPage
    latexSnippets.foreach(_.print(using lang, builder))
    printBaseModules()
    printSpecializationModules()
    printElectiveModules()
    assumeConsumption()
    builder.append("\\end{document}")
  }

  private def printBaseModules(): Unit = {
    renderingContext = RenderingContext.Mandatory
    printModules("Module", baseModules())
  }

  private def printElectiveModules(): Unit = {
    renderingContext = RenderingContext.Elective
    printModules("Wahlmodule", electiveModules())
  }

  private def printSpecializationModules(): Unit =
    pos.filter(_.specialization.isDefined).sortBy(_.specialization.get.deLabel).foreach { spec =>
      val specialization = spec.specialization.get
      renderingContext = RenderingContext.FieldOfStudy(specialization.id)
      printModules(s"Module im Schwerpunkt ``${specialization.deLabel}''", specializationModules(specialization.id))
    }

  private def consume(id: UUID): Unit =
    consumedModules += id

  private def assumeConsumption(): Unit = {
    val errs = printedModules.flatMap(_._1.id).filterNot(consumedModules.contains)
    if errs.nonEmpty then {
      logger.error(s"non consumed printModules: ${errs.toList}")
    }
  }

  /*
    Modules of all kinds (parent, child, generic, …) which belong to the current PO.
    Sorted by recommended semester and module title
   */
  private def baseModules() =
    modulesInPO
      .filter { module =>
        val po = module._1.metadata.po
        po.mandatory.exists(a => a.po == currentPO.id && a.specialization.isEmpty)
      }
      .sortBy { module =>
        val po                  = module._1.metadata.po.mandatory
        val recommendedSemester = po.filter(_.po == currentPO.id).flatMap(_.recommendedSemester).distinct
        val title               = module._1.metadata.title
        if recommendedSemester.isEmpty then (Int.MaxValue, title) else (recommendedSemester.min, title)
      }

  /*
    Modules of all kinds (parent, child, generic, …) which belong to the specialization of the current PO.
    Sorted by recommended semester and module title
   */
  private def specializationModules(specialization: String) =
    modulesInPO
      .filter { module =>
        val po = module._1.metadata.po
        // consider using mandatory and elective entries here
        po.mandatory.exists(a => a.po == currentPO.id && a.specialization.contains(specialization))
      }
      .sortBy { module =>
        val po                  = module._1.metadata.po.mandatory
        val recommendedSemester =
          po.filter(_.specialization.contains(specialization)).flatMap(_.recommendedSemester).distinct
        val title = module._1.metadata.title
        if recommendedSemester.isEmpty then (Int.MaxValue, title) else (recommendedSemester.min, title)
      }

  /*
    Modules of all kinds (parent, child, generic, …) which are electives of the current PO.
    Sorted by recommended semester and module title
   */
  private def electiveModules() =
    modulesInPO
      .filter { module =>
        val po = module._1.metadata.po
        po.optional.exists(a => a.po == currentPO.id)
      }
      .sortBy { module =>
        val po                  = module._1.metadata.po.optional
        val recommendedSemester = po.filter(_.po == currentPO.id).flatMap(_.recommendedSemester).distinct
        val title               = module._1.metadata.title
        if recommendedSemester.isEmpty then (Int.MaxValue, title) else (recommendedSemester.min, title)
      }

  /** Start each nonempty module group on a new page, with its first module directly below the heading. */
  private def printModules(sectionTitle: String, mods: Vector[(ModuleProtocol, LocalDate)]) =
    if mods.nonEmpty then {
      newPage
      section(sectionTitle)
      mods.foreach {
        case (m, lm) =>
          m.metadata.moduleRelation match {
            case Some(relation) =>
              val parentId = m.id.get
              printModule(m, lm, parent = None)
              newPage
              relation.children.toList
                .map { id =>
                  val child = printedModulesById.get(id)
                  if child.isEmpty then
                    logger.error(s"error while printing parent module ${m.id.get}: unable to find child module $id")
                  child
                }
                .collect { case Some(m) => m }
                .sortBy { m =>
                  val metadata            = m._1.metadata
                  val recommendedSemester =
                    metadata.po.mandatory
                      .flatMap(_.recommendedSemester)
                      .minOption
                      .orElse(
                        metadata.po.optional
                          .flatMap(_.recommendedSemester)
                          .minOption
                      )
                  (recommendedSemester.getOrElse(Int.MaxValue), metadata.title)
                }
                .foreach {
                  case (m, lm) =>
                    printModule(m, lm, parent = Some(parentId))
                    newPage
                }
            case None =>
              printModule(m, lm, parent = None)
              newPage
          }
      }
    }

  private def title() = {
    val studyProgram      = pos.find(_.po.id == currentPO.id).get
    val titleLabel        = strings.headline
    val studyProgramLabel = s"${escape(strings.label(studyProgram))} PO ${studyProgram.po.version}"
    val degreeLabel       = strings.description(studyProgram.degree)
    val semesterLabel     = semester.fold(strings.previewLabel)(s => s"\\LARGE ${strings.label(s)} ${s.year}")
    builder.append(
      s"""
         |\\begin{titlepage}
         |\\begin{figure}[h]
         |\\raggedleft
         |\\includegraphics[width=3cm]{./thk-logo.pdf}
         |\\end{figure}
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
         |\\end{titlepage}
         |""".stripMargin
    )
  }

  private def packages() =
    builder
      .append("""
                |% packages
                |\usepackage[english, ngerman]{babel}
                |\usepackage[a4paper, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}
                |\usepackage{graphicx} % include images
                |\usepackage[table]{xcolor} % table colors
                |\usepackage{array} % custom table columns
                |\usepackage{ltablex} % tables created with tabularx automatically gains longtable’s ability to break across pages
                |\usepackage{multirow} % tables with vertically merged cells generated by pandoc
                |\usepackage{pdflscape} % rotate landscape pages in the PDF
                |\usepackage{booktabs} % typographically correct horizontal rules for tables
                |\renewcommand{\arraystretch}{1.2}
                |\setlength{\LTcapwidth}{\textwidth} % allow long table captions to use the full text width and avoid unnecessary line breaks
                |\keepXColumns % lock the layout of flexible X columns so that multi-page tables stay aligned
                |\newcommand{\subsectiondivider}{% % imitates \midrule command
                |  \vspace{-.5em}
                |  \noindent\rule{\linewidth}{\lightrulewidth}%
                |  \vspace{-1.0em}
                |}
                |\usepackage[titles]{tocloft} % configure TOC columns while keeping the existing title style
                |\usepackage{hyperref} % support for hyperlinks
                |\usepackage{xurl} % line breaking in urls
                |\usepackage{titlesec}
                |\usepackage{fancyhdr} % customize the page header
                |\usepackage{parskip} % customize paragraph style
                |\usepackage{calc} % for calculating the width of the table
                |\usepackage{float} % for the H option of the figure environment""".stripMargin)
      .appendOpt(
        Option.when(isPreview)(
          s"""
             |\\usepackage[colorspec=0.9,text=${strings.previewLabel}]{draftwatermark} % watermark""".stripMargin
        )
      )

  private def headlineFormats(implicit builder: StringBuilder) =
    builder
      .append("""% compact headings; module content remains on separate lines even at deeper levels
                |\titleformat{\section}[hang]{\normalfont\LARGE\bfseries}{\thesection}{1em}{}
                |\titlespacing*{\section}{0pt}{2ex}{1.5ex}
                |\titleformat{\paragraph}[hang]{\normalfont\normalsize\bfseries}{\theparagraph}{1em}{}
                |\titlespacing*{\paragraph}{0pt}{3.25ex plus 1ex minus .2ex}{1.5ex plus .2ex}
                |\titleformat{\subparagraph}[hang]{\normalfont\normalsize\bfseries}{\thesubparagraph}{1em}{}
                |\titlespacing*{\subparagraph}{0pt}{3.25ex plus 1ex minus .2ex}{1.5ex plus .2ex}
                |""".stripMargin)

  private def commands() =
    builder
      .append(
        """
          |% commands and settings
          |\setcounter{tocdepth}{3} % include module groups, modules and child modules
          |\setcounter{secnumdepth}{3} % number sections, subsections and subsubsections
          |% Compact TOC: titles advance by 1.7em per level; number columns fit 99.99.99.
          |\cftsetindents{section}{0em}{1.8em}
          |\cftsetindents{subsection}{0.5em}{3em}
          |\cftsetindents{subsubsection}{1em}{4.2em}
          |\renewcommand{\cftsecpresnum}{\hfill}
          |\renewcommand{\cftsecaftersnum}{\enspace}
          |\renewcommand{\cftsubsecpresnum}{\hfill}
          |\renewcommand{\cftsubsecaftersnum}{\enspace}
          |\renewcommand{\cftsubsubsecpresnum}{\hfill}
          |\renewcommand{\cftsubsubsecaftersnum}{\enspace}
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
      )

  // Content headings stay unnumbered and out of the TOC, below the module or child-module heading.
  private def rewriteContentHeadings(origin: String) = {
    val pattern = """(?m)^\\(subsubsection|paragraph|subparagraph)\{([^}]*)\}""".r
    val result  = new StringBuilder()
    var lastEnd = 0

    for (m <- pattern.findAllMatchIn(origin)) {
      val headingName = m.group(2)
      result.append(origin.substring(lastEnd, m.start))
      if headingName == strings.learningOutcomeModuleCatalogLabel then {
        // reduce the gap between the bottom rule of the upper table and the “Learning Outcome” heading
        result.append("\\vspace{-2em}\n")
      } else if headingName == strings.moduleContentModuleCatalogLabel ||
        headingName == strings.teachingAndLearningMethodsModuleCatalogLabel ||
        headingName == strings.recommendedReadingModuleCatalogLabel
      then {
        // insert a horizontal line before each subsequent subsection
        result.append("\\subsectiondivider\n")
      }
      result.append(s"\\${m.group(1)}*{$headingName}")
      lastEnd = m.end
    }

    result.append(origin.substring(lastEnd))
    result.result()
  }

  private def printTableRow(key: String, value: String, isLast: Boolean = false): Unit = {
    if isLast then builder.append(s"$key & $value \\\\\n")
    else builder.append(s"$key & $value \\\\\\midrule\n")
  }

  private def printTable(tableIndex: "first" | "second", printRows: () => Unit) = {
    // the first table shows solid top rule and soft bottom rule
    // the second table shows soft top rule and solid bottom rule
    val (topRule, botRule) = tableIndex match {
      case "first"  => "toprule" -> "midrule"
      case "second" => "midrule" -> "bottomrule"
    }
    // create a two-column table that spans the full text width, where the first column is a fixed-width (5.5 cm)
    // left-aligned cell using normal body font and the second column (X) automatically expands to fill the remaining
    // horizontal space, with tidy spacing between and no extra margins at the table edges
    builder.append(
      s"\\begin{tabularx}{\\linewidth}{@{}>{\\normalfont\\normalsize\\raggedright\\arraybackslash}p{5.5cm}@{\\hspace{0.4em}}X@{}}\n\\$topRule\n"
    )
    printRows()
    builder.append(s"\\$botRule\n\\end{tabularx}\n")
  }

  private def printModule(module: ModuleProtocol, lastModified: LocalDate, parent: Option[UUID]): Unit = {
    consume(module.id.get)
    val languages         = payload.languages
    val seasons           = payload.seasons
    val people            = payload.people
    val assessmentMethods = payload.assessmentMethods
    val studyPrograms     = payload.studyPrograms

    def poRow = {
      def missingPO(po: String) = {
        logger.error(s"skipping unavailable po relation: module=${module.id.get} po=$po")
        Option.empty[String]
      }

      val mandatory = module.metadata.po.mandatory
        .filterNot(_.po == currentPO.id)
        .sortBy(_.po)
        .flatMap { po =>
          studyPrograms
            .find(_.fullPoId.id == po.fullPo)
            .map { studyProgram =>
              val spLabel   = escape(strings.label(studyProgram, studyProgram.specialization))
              val semesters = Option
                .when(po.recommendedSemester.nonEmpty)(
                  s" (Sem. ${fmtCommaSeparated(po.recommendedSemester.sorted)(_.toString())})"
                )
                .getOrElse("")
              s"$spLabel PO-${studyProgram.po.version}$semesters"
            }
            .orElse(missingPO(po.fullPo))
        }
      val optional = module.metadata.po.optional
        .filterNot(_.po == currentPO.id)
        .sortBy(_.po)
        .flatMap { po =>
          studyPrograms
            .find(_.fullPoId.id == po.fullPo)
            .map { studyProgram =>
              val spLabel = escape(strings.label(studyProgram, studyProgram.specialization))
              s"$spLabel PO-${studyProgram.po.version} (Wahlmodul)"
            }
            .orElse(missingPO(po.fullPo))
        }

      (mandatory ++ Option.when(mandatory.nonEmpty && optional.nonEmpty)(
        "\\rule[0.6ex]{.3\\linewidth}{0.1pt}"
      ) ++ optional).mkString("\\newline ") match {
        case ""    => strings.noneLabel
        case value => value
      }
    }

    def prerequisitesLabelRow(p: Option[ModulePrerequisiteEntryProtocol], currentModule: UUID) =
      p match
        case Some(p) =>
          val builder = new StringBuilder()
          if p.text.nonEmpty then {
            builder.append(escape(p.text))
          }
          if p.modules.nonEmpty then {
            val subBuilder  = new StringBuilder()
            val moduleLabel = messages("latex.module_catalog.module.label")
            subBuilder.append(s"$moduleLabel: ")
            p.modules.zipWithIndex.foreach {
              case (m, i) =>
                if printedModulesById.contains(m) then subBuilder.append(nameRef(m))
                else {
                  payload.modules.find(_.id == m) match {
                    case Some(module) =>
                      subBuilder.append(escape(module.title))
                    case None =>
                      logger.error(
                        s"error while printing parent module $currentModule: unable to find prerequisite module $m"
                      )
                  }
                }
                if i < p.modules.size - 1 then subBuilder.append(", ")
            }
            if builder.nonEmpty then {
              builder.append("\\newline ")
            }
            builder.append(subBuilder.toString())
          }
          builder.toString()
        case None => strings.noneLabel

    def attendanceRequirementRow(att: Option[AttendanceRequirement]) =
      att match
        case Some(att) if att.min.nonEmpty => escape(att.min)
        case _                             => strings.noneLabel

    def assessmentPrerequisiteRow(ass: Option[AssessmentPrerequisite]) =
      ass match
        case Some(ass) if ass.modules.nonEmpty => escape(ass.modules)
        case _                                 => strings.noneLabel

    def assessmentMethodsRow =
      if module.metadata.assessmentMethods.mandatory.isEmpty then strings.noneLabel
      else
        fmtCommaSeparated(module.metadata.assessmentMethods.mandatory.sortBy(_.method), "\\newline ") { a =>
          val method = strings.label(assessmentMethods.find(_.id == a.method))
          a.percentage.fold(method)(d => s"$method (${fmtDouble(d)} \\%)")
        }

    def recommendedSemesterRow = {
      val recommendedSemester = this.renderingContext match {
        case RenderingContext.Mandatory =>
          module.metadata.po.mandatory.filter(_.po == currentPO.id).flatMap(_.recommendedSemester).distinct
        case RenderingContext.Elective =>
          module.metadata.po.optional.filter(_.po == currentPO.id).flatMap(_.recommendedSemester).distinct
        case RenderingContext.FieldOfStudy(fullPo) =>
          // consider using mandatory and elective entries here
          module.metadata.po.mandatory.filter(_.fullPo == fullPo).flatMap(_.recommendedSemester).distinct
        case RenderingContext.None =>
          Nil
      }
      if recommendedSemester.isEmpty then strings.unknownLabel
      else s"${recommendedSemester.sorted.map(s => s"$s.").mkString(", ")} ${strings.semesterLabel}"
    }

    def durationRow = s"${module.metadata.duration} ${strings.semesterLabel}"

    sectionWithRef(module.metadata.title, module.id, parent.nonEmpty)

    // print the first part of the table
    printTable(
      "first",
      { () =>
        printTableRow(strings.moduleAbbrevLabel, escape(module.metadata.abbrev))
        printTableRow(strings.moduleTitleLabel, escape(module.metadata.title))
        printTableRow(strings.moduleTypeLabel, currentModuleType(module.metadata, parent))
        printTableRow(strings.ectsLabel, fmtDouble(module.metadata.ects))
        printTableRow(strings.languageLabel, strings.label(languages.find(_.id == module.metadata.language)))
        printTableRow(strings.durationLabel, durationRow)
        printTableRow(strings.recommendedSemesterLabel, recommendedSemesterRow)
        printTableRow(strings.frequencyLabel, strings.label(seasons.find(_.id == module.metadata.season)))
        printTableRow(
          strings.moduleCoordinatorLabel,
          fmtCommaSeparated(
            people.filter(p => module.metadata.moduleManagement.exists(_ == p.id)).sorted,
            "\\newline "
          )(fmtIdentity)
        )
        printTableRow(
          strings.lecturersLabel,
          fmtCommaSeparated(people.filter(p => module.metadata.lecturers.exists(_ == p.id)).sorted, "\\newline ")(
            fmtIdentity
          ),
          isLast = true
        )
      }
    )
    // continue with text
    moduleContent(
      module.id,
      module.metadata.language,
      module.deContent,
      module.enContent,
      parent.nonEmpty,
      List(
        (strings.learningOutcomeModuleCatalogLabel, GenLens[ModuleContent](_.learningOutcome)),
        (strings.moduleContentModuleCatalogLabel, GenLens[ModuleContent](_.content)),
        (strings.teachingAndLearningMethodsModuleCatalogLabel, GenLens[ModuleContent](_.teachingAndLearningMethods)),
        (strings.recommendedReadingModuleCatalogLabel, GenLens[ModuleContent](_.recommendedReading)),
      )
    )
    // print the second part of the table
    printTable(
      "second",
      { () =>
        printTableRow(strings.assessmentMethodLabel, assessmentMethodsRow)
        val (workload, contactHour, selfStudy) =
          strings.workloadLabels(module.metadata.workload, module.metadata.ects, currentPO.ectsFactor)
        printTableRow(workload._1, workload._2)
        printTableRow(contactHour._1, contactHour._2)
        printTableRow(selfStudy._1, selfStudy._2)
        printTableRow(
          strings.recommendedPrerequisitesLabel,
          prerequisitesLabelRow(module.metadata.prerequisites.recommended, module.id.get)
        )
        printTableRow(
          strings.requiredPrerequisitesLabel,
          prerequisitesLabelRow(module.metadata.prerequisites.required, module.id.get)
        )
        printTableRow(
          strings.attendanceRequirementLabel,
          attendanceRequirementRow(module.metadata.attendanceRequirement)
        )
        printTableRow(
          strings.assessmentPrerequisiteLabel,
          assessmentPrerequisiteRow(module.metadata.assessmentPrerequisite)
        )
        printTableRow(strings.poLabelShort, poRow)
        printTableRow(
          strings.particularitiesModuleCatalogLabel,
          particularitiesToLatex(
            module.id.get,
            module.metadata.language,
            module.deContent.particularities,
            module.enContent.particularities,
            parent.nonEmpty
          )
        )
        printTableRow(
          strings.lastModifiedLabel,
          lastModified.format(localDatePattern),
          isLast = !isPreview
        )
        if isPreview then {
          printTableRow("ID", module.id.get.toString, isLast = true)
        }
      }
    )
  }

  private def contentForLanguage(language: String, deContent: => String, enContent: => String) =
    if ModuleLanguage.isGerman(language) then deContent
    else if ModuleLanguage.isEnglish(language) then enContent
    else deContent // fallback to German if both are supported

  // does not support highlighting
  private def particularitiesToLatex(
      id: UUID,
      language: String,
      deContent: => String,
      enContent: => String,
      isChild: Boolean
  ): String = {
    val content = contentForLanguage(language, deContent, enContent)
    if content.nonEmpty && !content.forall(_.isWhitespace) then {
      printer.toLatex(content, headingShift = if isChild then 2 else 1).map(rewriteContentHeadings) match {
        case Left((e, stdErr)) =>
          logger.error(
            s"""content conversation from markdown to latex failed on $id:
               |  - throwable: ${e.getMessage}
               |  - sdtErr: $stdErr""".stripMargin
          )
          "ERROR"
        case Right(text) =>
          text
      }
    } else {
      strings.noneLabel
    }
  }

  private def moduleContent(
      id: Option[UUID],
      language: String,
      deContent: ModuleContent,
      enContent: ModuleContent,
      isChild: Boolean,
      entries: List[(String, Lens[ModuleContent, String])]
  ): Unit = {
    val markdownContent = new StringBuilder()
    entries.foreach {
      case (headline, lens) =>
        val content = contentForLanguage(language, lens.get(deContent), lens.get(enContent))
        if content.nonEmpty && !content.forall(_.isWhitespace) then {
          markdownContent.append(s"## $headline\n")
          markdownContent.append(content)
          markdownContent.append("\n\n")
        }
    }
    printer
      .toLatex(markdownContent.toString(), headingShift = if isChild then 2 else 1)
      .map(rewriteContentHeadings) match {
      case Left((e, stdErr)) =>
        logger.error(
          s"""content conversation from markdown to latex failed on $id:
             |  - throwable: ${e.getMessage}
             |  - sdtErr: $stdErr""".stripMargin
        )
        builder.append("ERROR\n\n")
      case Right(text) =>
        builder.append(text)
    }
  }

  private def sectionWithRef(text: String, id: Option[UUID], isChild: Boolean)(implicit builder: StringBuilder) = {
    val title = escape(text)
    id match
      case Some(id) =>
        val ref = s"\\label{sec:${id.toString}}"
        if isChild then builder.append(s"\\subsubsection{$title}$ref\n")
        else builder.append(s"\\subsection{$title}$ref\n")
      case None =>
        if isChild then builder.append(s"\\subsubsection{$title}\n") else builder.append(s"\\subsection{$title}\n")
  }

}
