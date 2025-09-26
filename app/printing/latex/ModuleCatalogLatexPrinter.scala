package printing.latex

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.UUID

import scala.collection.mutable.ListBuffer

import catalog.Semester
import cats.data.NonEmptyList
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
import printing.latex.snippet.LatexContentSnippet
import printing.latex.RenderingContext.Parent
import printing.pandoc.PandocApi
import printing.LocalizedStrings
import service.modulediff.ModuleProtocolDiff

private enum RenderingContext {
  case Mandatory
  case Elective
  case FieldOfStudy(po: String)
  case Parent(children: NonEmptyList[UUID])
  case None
}

object ModuleCatalogLatexPrinter {
  def chapter(name: String)(implicit builder: StringBuilder) =
    builder.append(s"\\chapter{$name}\n")

  def newPage(implicit builder: StringBuilder) =
    builder.append("\\newpage\n")

  def nameRef(module: UUID) =
    s"\\nameref{sec:${module.toString}}"

  def preview(
      pandocApi: PandocApi,
      messagesApi: MessagesApi,
      diffsForModule: UUID => Option[Set[String]],
      latexSnippets: List[LatexContentSnippet],
      pos: Seq[StudyProgramView],
      currentPO: PO,
      modules: Seq[(ModuleProtocol, Option[LocalDateTime])],
      payload: Payload,
      lang: Lang,
  ) = {
    new ModuleCatalogLatexPrinter(
      pandocApi,
      messagesApi,
      None,
      pos,
      currentPO,
      modules,
      payload,
      latexSnippets,
      Some(diffsForModule)
    )(using lang)
  }

  // TODO: make the same adjustments here
  def default(
      pandocApi: PandocApi,
      messagesApi: MessagesApi,
      semester: Semester,
      pos: Seq[StudyProgramView],
      currentPO: PO,
      modules: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])],
      payload: Payload,
      lang: Lang
  ) =
    new ModuleCatalogLatexPrinter(
      pandocApi,
      messagesApi,
      Some(semester),
      pos,
      currentPO,
      modules,
      payload,
      Nil,
      None
    )(using lang)
}

/**
 * Style from: https://www.overleaf.com/learn/latex/Page_size_and_margins
 */
final class ModuleCatalogLatexPrinter(
    pandocApi: PandocApi,
    messages: MessagesApi,
    semester: Option[Semester],
    pos: Seq[StudyProgramView],
    currentPO: PO,
    modulesInPO: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])],
    payload: Payload,
    latexSnippets: List[LatexContentSnippet],
    diffsForModule: Option[UUID => Option[Set[String]]]
)(using lang: Lang)
    extends Logging {

  import ModuleCatalogLatexPrinter.chapter
  import ModuleCatalogLatexPrinter.nameRef
  import ModuleCatalogLatexPrinter.newPage

  private given builder: StringBuilder = new StringBuilder()

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy", lang.locale)

  private val consumedModules = ListBuffer[UUID]()

  private var renderingContext = RenderingContext.None

  private def isPreview = semester.isEmpty

  private val strings = new LocalizedStrings(messages)

  private def currentModuleType(m: MetadataProtocol) =
    renderingContext match {
      case RenderingContext.Mandatory | RenderingContext.FieldOfStudy(_) => "Pflichtmodul"
      case RenderingContext.Elective =>
        val baseStr    = "Wahlmodul"
        val optionalPO = m.po.optional.filter(_.po == currentPO.id)

        if optionalPO.isEmpty then baseStr
        else s"$baseStr (${optionalPO.map(m => nameRef(m.instanceOf)).mkString(", ")})"
      case RenderingContext.Parent(children) =>
        val submodules = children.map(nameRef).toList.mkString(", ")
        s"Obermodul von $submodules"
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
    builder.append("\\documentclass[article, 11pt, oneside]{book}")
    packages(semester.isEmpty)
    commands()
    builder.append(s"""
                      |\\begin{document}
                      |\\selectlanguage{${strings.languagePackage}}""".stripMargin)
    title()
    newPage
    builder.append("\\tableofcontents\n")
    newPage
    headlineFormats
    latexSnippets.foreach(_.print(using lang, builder))
    printBaseModules()
    printSpecializationModules()
    printElectiveModules()
    printParentModules()
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

  private def consume(id: UUID) = {
    if !consumedModules.contains(id) then {
      consumedModules += id
    }
  }

  private def assumeConsumption(): Unit = {
    val errs = ListBuffer[UUID]()
    modulesInPO.foreach { module =>
      val moduleId = module._1.id.get
      if !consumedModules.contains(moduleId) then {
        errs += moduleId
      }
    }
    if errs.nonEmpty then {
      logger.error(s"non consumed printModules: ${errs.toList}")
    }
  }

  private def moduleType(m: MetadataProtocol) = {
    m.po.mandatory.exists(a => a.po == currentPO.id && a.specialization.isDefined) || m.po.mandatory.exists(a =>
      a.po == currentPO.id
    )
  }

  private def printParentModules(): Unit = {
    val modules = modulesInPO
      // TODO remove this filter
      .filter(a =>
        a._1.metadata.moduleRelation.fold(false) {
          case ModuleRelationProtocol.Parent(children) => true
          case ModuleRelationProtocol.Child(parent)    => false
        }
      )
      .sortBy(_._1.metadata.title)

    if modules.nonEmpty then {
      chapter("Obermodule")
      newPage
      modules
        .foreach {
          case (m, lm) =>
            m.metadata.moduleRelation.get match {
              case ModuleRelationProtocol.Child(_) =>
              case ModuleRelationProtocol.Parent(children) =>
                renderingContext = RenderingContext.Parent(children)
                printModule(m, lm, isChild = false)
                newPage
            }
        }
    }
  }

  private def baseModules() =
    modulesInPO
      // TODO remove this filter
      .filter(a =>
        a._1.metadata.moduleRelation.fold(true) {
          case ModuleRelationProtocol.Parent(children) => false
          case ModuleRelationProtocol.Child(parent)    => true
        }
      )
      .filterNot { module =>
        val po = module._1.metadata.po
        po.mandatory.exists(a => a.po == currentPO.id && a.specialization.isDefined) || po.optional.exists(a =>
          a.po == currentPO.id
        )
      }
      .sortBy { module =>
        val po                  = module._1.metadata.po.mandatory
        val recommendedSemester = po.filter(_.po == currentPO.id).flatMap(_.recommendedSemester).distinct
        val title               = module._1.metadata.title
        if recommendedSemester.isEmpty then (Int.MaxValue, title) else (recommendedSemester.min, title)
      }

  private def specializationModules(specialization: String) =
    modulesInPO
      // TODO remove this filter
      .filter(a =>
        a._1.metadata.moduleRelation.fold(true) {
          case ModuleRelationProtocol.Parent(children) => false
          case ModuleRelationProtocol.Child(parent)    => true
        }
      )
      .filter { module =>
        val po = module._1.metadata.po
        // consider using mandatory and elective entries here
        po.mandatory.exists(a => a.po == currentPO.id && a.specialization.contains(specialization))
      }
      .sortBy { module =>
        val po = module._1.metadata.po.mandatory
        val recommendedSemester =
          po.filter(_.specialization.contains(specialization)).flatMap(_.recommendedSemester).distinct
        val title = module._1.metadata.title
        if recommendedSemester.isEmpty then (Int.MaxValue, title) else (recommendedSemester.min, title)
      }

  private def electiveModules() =
    modulesInPO
      // TODO remove this filter
      .filter(a =>
        a._1.metadata.moduleRelation.fold(true) {
          case ModuleRelationProtocol.Parent(children) => false
          case ModuleRelationProtocol.Child(parent)    => true
        }
      )
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

  private def printModules(chapterTitle: String, mods: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])]) = {
    chapter(chapterTitle)
    newPage
    go(mods)
  }

  private def go(mods: Seq[(ModuleProtocol, LocalDateTime | Option[LocalDateTime])]) = {
    if (mods.isEmpty) newPage
    else
      mods
        .foreach {
          case (m, lm) =>
            m.metadata.moduleRelation match {
              case Some(ModuleRelationProtocol.Child(_)) =>
                printModule(m, lm, isChild = false)
                newPage
              case Some(ModuleRelationProtocol.Parent(children)) =>
//                printModule(m, lm, isChild = false)
//                newPage
//                children.toList
//                  .map { id =>
//                    val printModule = mods.find(_._1.id.contains(id))
//                    if printModule.isEmpty then logger.error(s"unable to find child printModule $id from parent ${m.id}")
//                    printModule
//                  }
//                  .collect { case Some(m) => m }
//                  .sortBy(_._1.metadata.title)
//                  .foreach {
//                    case (m, lm) =>
//                      printModule(m, lm, isChild = true)
//                      newPage
//                  }
              case None =>
                printModule(m, lm, isChild = false)
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

  private def packages(isDraft: Boolean) =
    builder
      .append("""
                |% packages
                |\usepackage[english, ngerman]{babel}
                |\usepackage[a4paper, total={16cm, 24cm}, left=2.5cm, right=2.5cm, top=2.5cm, bottom=2.5cm]{geometry}
                |\usepackage{longtable} % needed to support markdown tables
                |\usepackage{booktabs} % needed to support markdown tables
                |\usepackage{layout}
                |\usepackage{tabularx}
                |\usepackage{hyperref} % support for hyperlinks
                |\usepackage{xurl} % line breaking in urls
                |\usepackage{titlesec}
                |\usepackage{fancyhdr} % customize the page header
                |\usepackage{parskip} % customize paragraph style""".stripMargin)
      .appendOpt(
        Option.when(isDraft)(
          s"""
             |\\usepackage[colorspec=0.9,text=${strings.previewLabel}]{draftwatermark} % watermark
             |\\usepackage[defaultcolor=orange]{changes} % highlights changes (https://ctan.org/pkg/changes?lang=en)""".stripMargin
        )
      )

  private def headlineFormats(implicit builder: StringBuilder) =
    builder
      .append("""% define the chapter format
                |\titleformat{\chapter}[display]
                |{\normalfont\Huge\bfseries} % font attributes
                |{\vspace*{\fill}} % vertical space before the chapter title
                |{0pt} % horizontal space between the chapter title and the left margin
                |{\Huge\centering} % font size of the chapter title
                |[\vspace*{\fill}] % vertical space after the chapter title
                |""".stripMargin)
//      .append("% define the subsection format\n")
//      .append("\\titleformat{name=\\subsection}\n")
//      .append("{\\normalfont\\large\\bfseries} % default font attributes\n")
//      .append("{} % remove numbers\n")
//      .append("{0pt} % no space between subsection and left margin\n")
//      .append("{} % nothing after subsection\n")

  private def commands() =
    builder
      .append(
        """
          |% commands and settings
          |\setcounter{tocdepth}{2} % set tocdepth to 2 (includes chapters, sections (printModules) and subsections (child printModules))
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

  private def printModule(
      module: ModuleProtocol,
      lastModified: LocalDateTime | Option[LocalDateTime],
      isChild: Boolean
  ): Unit = {
    consume(module.id.get)

    def row(key: String, value: String) =
      builder.append(s"$key & $value \\\\\n")

    val moduleTypes       = payload.moduleTypes
    val languages         = payload.languages
    val seasons           = payload.seasons
    val people            = payload.people
    val assessmentMethods = payload.assessmentMethods
    val studyPrograms     = payload.studyPrograms

    val diffs = diffsForModule.flatMap(_.apply(module.id.get))

    def highlightIf(str: String, p: String => Boolean) =
      if diffs.exists(_.exists(p)) then highlight(str) else str

    def poRow = {
      val mandatorySize = module.metadata.po.mandatory.size
      val electiveSize  = module.metadata.po.optional.size

      // assumes that the current PO is definitely included in either of mandatory or optional
      (mandatorySize, electiveSize) match {
        case (1, 0) => strings.noneLabel
        case (0, 1) => strings.noneLabel
        case (0, 0) =>
          logger.error(s"expected module to be part of some po relationship of po ${currentPO.id}")
          strings.unknownLabel
        case _ =>
          // remove ourselves for rendering
          val mandatory = module.metadata.po.mandatory.filterNot(p => p.po == currentPO.id).sortBy(_.po)
          val optional  = module.metadata.po.optional.filterNot(p => p.po == currentPO.id).sortBy(_.po)

          // use this code if specializations of the same study program should be rendered in "In anderen Studiengängen"
//          val (mandatory, optional) = renderingContext match {
//            case RenderingContext.Mandatory | RenderingContext.Elective | RenderingContext.None =>
//              val mandatory = module.metadata.po.mandatory.filterNot(p => p.po == currentPO.id).sortBy(_.po)
//              val optional  = module.metadata.po.optional.filterNot(p => p.po == currentPO.id).sortBy(_.po)
//              (mandatory, optional)
//            case RenderingContext.FieldOfStudy(po) =>
//              val mandatory = module.metadata.po.mandatory.filterNot(p => p.fullPo == po).sortBy(_.po)
//              val optional  = module.metadata.po.optional.filterNot(p => p.fullPo == po).sortBy(_.po)
//              (mandatory, optional)
//          }

          val builder = new StringBuilder()

          for (po, i) <- mandatory.zipWithIndex yield {
            val content = studyPrograms.find(_.fullPoId.id == po.fullPo) match {
              case Some(studyProgram) =>
                val spLabel = escape(strings.label(studyProgram, studyProgram.specialization))
                var content = s"$spLabel PO-${studyProgram.po.version}"
                if po.recommendedSemester.nonEmpty then {
                  content += s" (Sem. ${fmtCommaSeparated(po.recommendedSemester.sorted)(_.toString())})"
                }
                content
              case None =>
                logger.error(s"expected po ${po.fullPo} to exists for module ${module.id.get}")
                highlight(s"NOT FOUND: ${escape(po.po)}")
            }
            builder.append(content)
            if i < mandatory.size - 1 then {
              builder.append("\\newline ")
            }
          }

          if mandatory.nonEmpty && optional.nonEmpty then {
            // rule at roughly baseline height which takes up 30 % of the line width
            builder.append("\\newline \\rule[0.6ex]{.3\\linewidth}{0.1pt} \\newline ")
          }

          for (po, i) <- optional.zipWithIndex yield {
            val content = studyPrograms.find(_.po.id == po.po) match {
              case Some(studyProgram) =>
                val spLabel = escape(strings.label(studyProgram, studyProgram.specialization))
                s"$spLabel PO-${studyProgram.po.version} (Wahlmodul)"
              case None =>
                logger.error(s"expected po ${po.fullPo} to exists for module ${module.id.get}")
                highlight(s"NOT FOUND: ${escape(po.po)}")
            }
            builder.append(content)
            if i < optional.size - 1 then {
              builder.append("\\newline ")
            }
          }

          if builder.isEmpty then strings.noneLabel else builder.toString()
      }
    }

    def prerequisitesLabelRow(p: Option[ModulePrerequisiteEntryProtocol]) =
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
                val module = payload.modules.find(_.id == m).get
                if modulesInPO.exists(_._1.id.get == m) then subBuilder.append(nameRef(m))
                else subBuilder.append(escape(module.title))
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
        case Some(att) =>
          val builder = new StringBuilder()
          if att.min.nonEmpty then {
            builder.append(escape(att.min))
          }
          if att.reason.nonEmpty then {
            if builder.nonEmpty then builder.append("\\,\\textbullet\\,")
            builder.append(s"Begründung: ${escape(att.reason)}")
          }
          if att.absence.nonEmpty then {
            if builder.nonEmpty then builder.append("\\,\\textbullet\\,")
            builder.append(s"Fehlzeiten: ${escape(att.absence)}")
          }
          if builder.isEmpty then strings.noneLabel else builder.toString()
        case None => strings.noneLabel

    def assessmentPrerequisiteRow(ass: Option[AssessmentPrerequisite]) =
      ass match
        case Some(ass) =>
          val builder = new StringBuilder()
          if ass.modules.nonEmpty then {
            builder.append(escape(ass.modules))
          }
          if ass.reason.nonEmpty then {
            if builder.nonEmpty then builder.append("\\,\\textbullet\\,")
            builder.append(s"Begründung: ${escape(ass.reason)}")
          }
          if builder.isEmpty then strings.noneLabel else builder.toString()
        case None => strings.noneLabel

    def assessmentMethodsRow =
      if module.metadata.assessmentMethods.mandatory.isEmpty then strings.noneLabel
      else
        fmtCommaSeparated(module.metadata.assessmentMethods.mandatory.sortBy(_.method), "\\newline ") { a =>
          val method = strings.label(assessmentMethods.find(_.id == a.method))
          a.percentage.fold(method)(d => s"$method (${fmtDouble(d)} \\%)")
        }

    def renderWorkloadRow = {
      val (workload, contactHour, selfStudy) =
        strings.workloadLabels(module.metadata.workload, module.metadata.ects, currentPO.ectsFactor)

      row(highlightIf(workload._1, ModuleProtocolDiff.isModuleWorkload), workload._2)
      row(highlightIf(contactHour._1, ModuleProtocolDiff.isModuleWorkload), contactHour._2)
      row(highlightIf(selfStudy._1, ModuleProtocolDiff.isModuleWorkload), selfStudy._2)
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
        case RenderingContext.Parent(children) =>
          // consider using mandatory and elective entries here
          module.metadata.po.mandatory.filter(_.po == currentPO.id).flatMap(_.recommendedSemester).distinct
        case RenderingContext.None =>
          Nil
      }
      if recommendedSemester.isEmpty then strings.unknownLabel
      else s"${recommendedSemester.sorted.map(s => s"$s.").mkString(", ")} ${strings.semesterLabel}"
    }

    def durationRow = s"${module.metadata.duration} ${strings.semesterLabel}"

    sectionWithRef(module.metadata.title, module.id, isChild)
    builder.append("\\begin{tabularx}{\\linewidth}{@{}>{\\bfseries}l@{\\hspace{.5em}}X@{}}\n")
    row(highlightIf(strings.moduleAbbrevLabel, ModuleProtocolDiff.isModuleAbbrev), escape(module.metadata.abbrev))
    row(highlightIf(strings.moduleTitleLabel, ModuleProtocolDiff.isModuleTitle), escape(module.metadata.title))
    row(strings.moduleTypeLabel, currentModuleType(module.metadata))
    row(highlightIf(strings.ectsLabel, ModuleProtocolDiff.isModuleEcts), fmtDouble(module.metadata.ects))
    row(
      highlightIf(strings.languageLabel, ModuleProtocolDiff.isModuleLanguage),
      strings.label(languages.find(_.id == module.metadata.language))
    )
    row(
      highlightIf(strings.durationLabel, ModuleProtocolDiff.isModuleDuration),
      durationRow
    )
    row(strings.recommendedSemesterLabel, recommendedSemesterRow)
    row(
      highlightIf(strings.frequencyLabel, ModuleProtocolDiff.isModuleSeason),
      strings.label(seasons.find(_.id == module.metadata.season))
    )
    row(
      highlightIf(strings.moduleCoordinatorLabel, ModuleProtocolDiff.isModuleModuleManagement),
      fmtCommaSeparated(
        people.filter(p => module.metadata.moduleManagement.exists(_ == p.id)).sorted,
        "\\newline "
      )(fmtIdentity)
    )
    row(
      highlightIf(strings.lecturersLabel, ModuleProtocolDiff.isModuleLecturers),
      fmtCommaSeparated(people.filter(p => module.metadata.lecturers.exists(_ == p.id)).sorted, "\\newline ")(
        fmtIdentity
      )
    )
    row(
      highlightIf(strings.assessmentMethodLabel, ModuleProtocolDiff.isModuleAssessmentMethodsMandatory),
      assessmentMethodsRow
    )
    renderWorkloadRow
    row(
      highlightIf(strings.recommendedPrerequisitesLabel, ModuleProtocolDiff.isModuleRecommendedPrerequisites),
      prerequisitesLabelRow(module.metadata.prerequisites.recommended)
    )
    row(
      highlightIf(strings.requiredPrerequisitesLabel, ModuleProtocolDiff.isModuleRequiredPrerequisites),
      prerequisitesLabelRow(module.metadata.prerequisites.required)
    )
    row(
      highlightIf(strings.attendanceRequirementLabel, ModuleProtocolDiff.isModuleAttendanceRequirement),
      attendanceRequirementRow(module.metadata.attendanceRequirement)
    )
    row(
      highlightIf(strings.assessmentPrerequisiteLabel, ModuleProtocolDiff.isModuleAssessmentPrerequisite),
      assessmentPrerequisiteRow(module.metadata.assessmentPrerequisite)
    )
    row(highlightIf(strings.poLabelShort, ModuleProtocolDiff.isPOMandatory), poRow)
    if isPreview then {
      row("ID", module.id.get.toString)
      row(
        strings.lastModifiedLabel,
        lastModified match
          case lm: LocalDateTime => lm.format(localDatePattern)
          case Some(lm)          => lm.format(localDatePattern)
          case None              => strings.unknownLabel
      )
    }

    builder.append("\\end{tabularx}\n")
    moduleContent(
      module.id,
      module.deContent,
      module.enContent,
      List(
        (strings.learningOutcomeLabel, GenLens[ModuleContent](_.learningOutcome)),
        (strings.moduleContentLabel, GenLens[ModuleContent](_.content)),
        (strings.teachingAndLearningMethodsLabel, GenLens[ModuleContent](_.teachingAndLearningMethods)),
        (strings.recommendedReadingLabel, GenLens[ModuleContent](_.recommendedReading)),
        (strings.particularitiesLabel, GenLens[ModuleContent](_.particularities))
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
        val content = if strings.isGerman then lens.get(deContent) else lens.get(enContent)
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
                    if subsection == strings.learningOutcomeLabel then ModuleProtocolDiff.learningOutcomeKey
                    else if subsection == strings.moduleContentLabel then ModuleProtocolDiff.moduleContentKey
                    else if subsection == strings.teachingAndLearningMethodsLabel then
                      ModuleProtocolDiff.teachingAndLearningMethodsKey
                    else if subsection == strings.recommendedReadingLabel then ModuleProtocolDiff.recommendedReadingKey
                    else if subsection == strings.particularitiesLabel then ModuleProtocolDiff.particularitiesKey
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
