package service.artifact.modulecatalog

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import database.repo.core.*
import database.view.StudyProgramViewRepository
import models.*
import ops.toFuture
import ops.FileOps.copy
import ops.FileOps.foreachFileOfDirectory
import printing.latex.snippet.*
import printing.latex.studyplan.StudyPlanSnippet
import printing.latex.MarkdownLatexPrinter
import printing.latex.ModuleCatalogLatexPrinter
import printing.latex.Payload
import service.core.IdentityService
import service.modulediff.ModuleProtocolDiff
import service.ModuleService
import settings.AppSettings
import cats.data.NonEmptyList
import cli.GitCLI
import cli.LatexCompiler.compile
import cli.LatexCompiler.getPdf
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import service.artifact.ModulePreview
import service.artifact.POModules

final class ModuleCatalogConfigException(message: String) extends IllegalArgumentException(message)

private final case class CatalogPreparation(
    modules: Vector[(ModuleProtocol, LocalDate)],
    latexSnippets: List[LatexContentSnippet],
    postTitleSnippets: List[LatexContentSnippet]
)

private[artifact] object ModuleCatalogService {
  private def mandatoryRelations(module: ModuleProtocol, currentPO: String): List[ModulePOMandatoryProtocol] =
    module.metadata.po.mandatory.filter(_.po == currentPO)

  private def recommendedSemestersForStudyPlan(module: ModuleProtocol, currentPO: String): List[Int] =
    mandatoryRelations(module, currentPO).flatMap(_.recommendedSemester).distinct.sorted

  private def recommendedSemestersForCatalog(module: ModuleProtocol, currentPO: String): List[Int] =
    (
      module.metadata.po.mandatory.filter(_.po == currentPO).flatMap(_.recommendedSemester) ++
        module.metadata.po.optional.filter(_.po == currentPO).flatMap(_.recommendedSemester)
    ).distinct.sorted

  private def duplicateIds(ids: List[UUID]): List[UUID] =
    ids.groupBy(identity).collect { case (id, values) if values.size > 1 => id }.toList

  /** Checks all overrides against the current PO data and reports validation errors together. */
  def validateConfig(
      currentPO: String,
      modules: Vector[ModuleProtocol],
      poOnly: Seq[StudyProgramView],
      config: ModuleCatalogConfig
  ): Unit = {
    val errors     = ListBuffer.empty[String]
    val moduleById = modules.map(module => module.id.get -> module).toMap

    def requireKnown(ids: List[UUID], label: String): Unit = {
      val unknown = ids.filterNot(moduleById.contains).distinct
      if unknown.nonEmpty then errors += s"$label references modules outside PO $currentPO: ${unknown.mkString(", ")}"
    }

    val moduleSelection = config.moduleSelection
    val studyPlan       = config.studyPlan

    requireKnown(moduleSelection.excludedModuleIds, "excludedModuleIds")
    requireKnown(
      moduleSelection.excludedElectiveOptions.flatMap(o => List(o.genericModuleId, o.optionModuleId)),
      "excludedElectiveOptions"
    )
    requireKnown(studyPlan.semesterSelections.map(_.moduleId), "semesterSelections")
    requireKnown(studyPlan.genericModuleOccurrences.map(_.moduleId), "genericModuleOccurrences")

    val excludedModuleIds           = moduleSelection.excludedModuleIds.toSet
    val excludedStudyPlanReferences =
      (
        studyPlan.semesterSelections.map(_.moduleId) ++
          studyPlan.genericModuleOccurrences.map(_.moduleId)
      ).filter(excludedModuleIds.contains).distinct
    if excludedStudyPlanReferences.nonEmpty then {
      errors +=
        s"studyPlan references excluded modules in PO $currentPO: ${excludedStudyPlanReferences.mkString(", ")}"
    }

    duplicateIds(studyPlan.semesterSelections.map(_.moduleId)).foreach { id =>
      errors += s"semesterSelections contains duplicate module id $id"
    }

    if poOnly.exists(_.specialization.isDefined) && studyPlan.sections.nonEmpty then {
      errors += s"studyPlan.sections cannot be used for PO $currentPO because it has specializations"
    }

    moduleSelection.excludedElectiveOptions.foreach { option =>
      moduleById.get(option.genericModuleId).foreach { generic =>
        if !generic.metadata.isGeneric then {
          errors += s"excludedElectiveOptions genericModuleId ${option.genericModuleId} is not a generic module"
        }
      }

      moduleById.get(option.optionModuleId).foreach { optionModule =>
        val relationshipExists = optionModule.metadata.po.optional
          .exists(optional => optional.po == currentPO && optional.instanceOf == option.genericModuleId)
        if !relationshipExists then {
          errors +=
            s"excludedElectiveOptions references missing relationship ${option.optionModuleId} -> ${option.genericModuleId} in PO $currentPO"
        }
      }
    }

    studyPlan.semesterSelections.foreach { selection =>
      moduleById.get(selection.moduleId).foreach { module =>
        val recommendedSemesters = recommendedSemestersForStudyPlan(module, currentPO)
        if mandatoryRelations(module, currentPO).isEmpty then {
          errors += s"semesterSelections module ${selection.moduleId} is not mandatory in PO $currentPO"
        } else if !recommendedSemesters.contains(selection.selectedSemester) then {
          errors +=
            s"semesterSelections module ${selection.moduleId} selects semester ${selection.selectedSemester}, expected one of ${recommendedSemesters.mkString(", ")}"
        }
      }
    }

    studyPlan.genericModuleOccurrences.foreach { occurrence =>
      moduleById.get(occurrence.moduleId).foreach { module =>
        val recommendedSemesters = recommendedSemestersForStudyPlan(module, currentPO)
        if !module.metadata.isGeneric then {
          errors += s"genericModuleOccurrences module ${occurrence.moduleId} is not a generic module"
        }
        if mandatoryRelations(module, currentPO).isEmpty then {
          errors += s"genericModuleOccurrences module ${occurrence.moduleId} is not mandatory in PO $currentPO"
        }
        if recommendedSemesters.isEmpty then {
          errors += s"genericModuleOccurrences module ${occurrence.moduleId} has no recommended semesters"
        } else if !recommendedSemesters.contains(occurrence.semester) then {
          errors +=
            s"genericModuleOccurrences module ${occurrence.moduleId} uses semester ${occurrence.semester}, expected one of ${recommendedSemesters.mkString(", ")}"
        }
        if occurrence.count <= 0 then {
          errors += s"genericModuleOccurrences module ${occurrence.moduleId} must have a positive count"
        }
      }
    }

    if errors.nonEmpty then {
      throw new ModuleCatalogConfigException(errors.mkString("; "))
    }
  }

  /** Applies module and elective exclusions, dropping modules with no remaining relation to the current PO. */
  def applyModuleSelection(
      currentPO: String,
      modules: Vector[(ModuleProtocol, LocalDate)],
      moduleSelection: ModuleCatalogModuleSelectionConfig
  ): Vector[(ModuleProtocol, LocalDate)] = {
    val excludedModuleIds = moduleSelection.excludedModuleIds.toSet

    modules
      .filterNot { case (module, _) => excludedModuleIds.contains(module.id.get) }
      .map {
        case (module, lastModified) =>
          val currentModuleId        = module.id.get
          val excludedGenericModules = moduleSelection.excludedElectiveOptions
            .filter(_.optionModuleId == currentModuleId)
            .map(_.genericModuleId)
            .toSet
          val filteredOptional = module.metadata.po.optional
            .filterNot(optional => optional.po == currentPO && excludedGenericModules.contains(optional.instanceOf))
          val filteredModule = module.copy(
            metadata = module.metadata.copy(
              po = module.metadata.po.copy(optional = filteredOptional)
            )
          )
          filteredModule -> lastModified
      }
      .filter { case (module, _) => module.metadata.po.hasPORelation(currentPO) }
  }

  /** Builds the available catalog overrides from preview modules and study-program metadata. */
  def configOptions(
      poId: String,
      modules: Vector[ModuleProtocol],
      studyPrograms: Seq[StudyProgramView]
  ): ModuleCatalogConfigOptions = {
    def moduleOption(module: ModuleProtocol): ModuleCatalogModuleOption = {
      val metadata = module.metadata
      ModuleCatalogModuleOption(
        id = module.id.get,
        title = metadata.title,
        abbrev = metadata.abbrev,
        ects = metadata.ects,
        moduleType = metadata.moduleType,
        recommendedSemesters = recommendedSemestersForCatalog(module, poId),
        mandatory = metadata.po.mandatory.exists(_.po == poId),
        optional = metadata.po.optional.exists(_.po == poId),
        specializations = (
          metadata.po.mandatory.filter(_.po == poId).flatMap(_.specialization) ++
            metadata.po.optional.filter(_.po == poId).flatMap(_.specialization)
        ).distinct,
        defaultIncluded = true
      )
    }

    val genericElectiveGroups = modules
      .filter(_.metadata.isGeneric)
      .map { genericModule =>
        val genericModuleId = genericModule.id.get
        val candidates      = modules
          .filter(module =>
            module.metadata.po.optional
              .exists(optional => optional.po == poId && optional.instanceOf == genericModuleId)
          )
          .map(m => {
            ModuleCatalogElectiveOptionCandidate(
              moduleId = m.id.get,
              title = m.metadata.title,
              abbrev = m.metadata.abbrev,
              ects = m.metadata.ects
            )
          })
        ModuleCatalogGenericElectiveGroup(
          genericModuleId,
          genericModule.metadata.title,
          genericModule.metadata.abbrev,
          candidates
        )
      }
      .filter(_.optionCandidates.nonEmpty)

    val specializations = studyPrograms
      .flatMap(_.specialization)
      .map(specialization => ModuleCatalogSpecializationOption(specialization.id, specialization.deLabel))
      .distinctBy(_.id)
      .toVector

    ModuleCatalogConfigOptions(
      modules.map(moduleOption),
      genericElectiveGroups,
      specializations
    )
  }

  def diagnosticsSnippets(isPreview: Boolean, warnings: List[ModuleCatalogWarning]): List[LatexContentSnippet] =
    Option.when(isPreview && warnings.nonEmpty)(new DiagnosticsContentSnippet(warnings)).toList
}

@Singleton
final class ModuleCatalogService @Inject() (
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    identityService: IdentityService,
    assessmentMethodRepo: AssessmentMethodRepository,
    poRepository: PORepository,
    messagesApi: MessagesApi,
    gitCLI: GitCLI,
    appSettings: AppSettings,
    implicit val ctx: ExecutionContext
) extends Logging {
  private def mcIntroPath: String  = appSettings.pandoc.mcIntroPath
  private def mcAssetsPath: String = appSettings.pandoc.mcAssetsPath
  private def texCommand: String   = appSettings.pandoc.texCmd

  private type ModuleDiffs = List[(ModuleCore, Set[String])]

  def create(po: String, latexFile: Path, semester: Semester, config: ModuleCatalogConfig): Future[Path] = {
    logger.info(s"creating module catalog for po $po")
    generateCatalog(po, latexFile, Some(semester), config)
  }

  def preview(po: String, latexFile: Path, config: ModuleCatalogConfig): Future[Path] = {
    logger.info(s"creating module catalog preview for po $po")
    generateCatalog(po, latexFile, None, config)
  }

  /** Loads the preview-backed configuration options for a PO. */
  def configOptions(po: String): Future[ModuleCatalogConfigOptions] =
    modulesFromPreview(po).flatMap(modules =>
      studyProgramsFor(po).map {
        case (_, poOnly) => ModuleCatalogService.configOptions(po, modules.modules.map(_._1), poOnly)
      }
    )

  private def modulesFromPreview(po: String): Future[POModules] =
    Future.fromTry(new ModulePreview(gitCLI).getByPO(po))

  private def prepare(
      po: String,
      workingDir: Path,
      rawModules: Vector[(ModuleProtocol, LocalDate)],
      poOnly: Seq[StudyProgramView],
      isPreview: Boolean,
      config: ModuleCatalogConfig
  ): CatalogPreparation = {
    ModuleCatalogService.validateConfig(po, rawModules.map(_._1), poOnly, config)
    val modules       = ModuleCatalogService.applyModuleSelection(po, rawModules, config.moduleSelection)
    val warnings      = ListBuffer.empty[ModuleCatalogWarning]
    val latexSnippets = introSnippet(workingDir, po).toList.appended(
      studyPlanSnippet(
        po,
        modules,
        isPreview,
        config.studyPlan,
        poOnly.flatMap(_.specialization).toList,
        warnings += _
      )
    )
    if !isPreview then logWarnings(po, warnings.toList)
    CatalogPreparation(
      modules,
      latexSnippets,
      ModuleCatalogService.diagnosticsSnippets(isPreview, warnings.toList)
    )
  }

  private def generateCatalog(po: String, latexFile: Path, semester: Option[Semester], config: ModuleCatalogConfig) = {
    val isPreview = semester.isEmpty
    val lang      = Lang(Locale.GERMANY)

    for {
      (all, poOnly) <- studyProgramsFor(po)
      allModules    <- modulesFromPreview(po)
      prep = prepare(po, latexFile.getParent, allModules.modules, poOnly, isPreview, config)
      _    = copyAssets(latexFile.getParent)
      content <- print(poOnly, prep.modules, all, lang, prep.latexSnippets, prep.postTitleSnippets, semester)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
  }

  /**
   * Returns all study programs and those which match the po.
   * @param po to check
   * @return left: all study programs, right: study programs matching po
   */
  private def studyProgramsFor(po: String): Future[(Seq[StudyProgramView], Seq[StudyProgramView])] =
    studyProgramViewRepo.notExpired().map { all =>
      val poOnly = all.filter(_.po.id == po)
      assume(poOnly.nonEmpty, s"expected study programs for po $po")
      (all, poOnly)
    }

  @unused
  private def diffs(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])],
      bannedGenericModules: List[UUID]
  ): ModuleDiffs =
    changedModules.foldLeft(Nil) {
      case (acc, (p, _)) =>
        if bannedGenericModules.contains(p.id.get) then acc
        else {
          val diffs = liveModules.find(_._1.id == p.id) match {
            case Some((existing, _)) =>
              val (_, diffs) = ModuleProtocolDiff.diff(
                existing.normalize(),
                p.normalize(),
                None,
                Set.empty
              )
              diffs
            case None =>
              Set("all")
          }
          acc.prepended((ModuleCore(p.id.get, p.metadata.title, p.metadata.abbrev), diffs))
        }
    }

  private def print(
      poOnly: Seq[StudyProgramView],
      modules: Vector[(ModuleProtocol, LocalDate)],
      studyPrograms: Seq[StudyProgramView],
      lang: Lang,
      latexSnippets: List[LatexContentSnippet],
      postTitleSnippets: List[LatexContentSnippet],
      semester: Option[Semester],
  ): Future[StringBuilder] = {
    val liveModules       = moduleService.allModuleCore()
    val createdModules    = moduleService.allNewlyCreated()
    val moduleTypes       = moduleTypeRepository.all()
    val languages         = languageRepository.all()
    val seasons           = seasonRepository.all()
    val people            = identityService.all()
    val assessmentMethods = assessmentMethodRepo.all()
    val currentPO         = poRepository.get(poOnly.head.po.id)

    for {
      liveModules       <- liveModules
      createdModules    <- createdModules
      moduleTypes       <- moduleTypes
      languages         <- languages
      seasons           <- seasons
      people            <- people
      assessmentMethods <- assessmentMethods
      currentPO         <- currentPO
    } yield {
      val payload = Payload(
        moduleTypes,
        languages,
        seasons,
        people,
        assessmentMethods,
        studyPrograms,
        liveModules ++ createdModules
      )
      val markdownLatexPrinter = new MarkdownLatexPrinter(texCommand)
      val printer              = semester match {
        case Some(value) =>
          ModuleCatalogLatexPrinter.default(
            markdownLatexPrinter,
            messagesApi,
            value,
            latexSnippets,
            postTitleSnippets,
            poOnly,
            currentPO,
            modules,
            payload,
            lang
          )
        case None =>
          // Module diffs are disabled by now. Consider enabling it if needed
          val diffs: ModuleDiffs = List.empty
          ModuleCatalogLatexPrinter.preview(
            markdownLatexPrinter,
            messagesApi,
            id => diffs.find(_._1.id == id).map(_._2),
            latexSnippets,
            postTitleSnippets,
            poOnly,
            currentPO,
            modules,
            payload,
            lang,
          )
      }
      printer.print()
    }
  }

  // TODO: same for prod catalog
  private def copyAssets(parentDir: Path): Unit =
    try {
      Paths.get(mcAssetsPath).foreachFileOfDirectory { path =>
        path.copy(parentDir).match {
          case Left(err) => throw Exception(s"failed to copy assets into media folder: $err")
          case Right(_)  =>
        }
      }
    } catch {
      case NonFatal(e) => throw Exception(e)
    }

  @unused
  private def layoutSnippet(preview: Boolean): Option[LatexContentSnippet] =
    Option.when(preview)(LayoutContentSnippet())

  @unused
  private def diffSnippet(moduleDiffs: ModuleDiffs): Option[LatexContentSnippet] =
    NonEmptyList.fromList(moduleDiffs).map(DiffContentSnippet(_, messagesApi))

  private def introSnippet(dir: Path, po: String): Option[LatexContentSnippet] =
    IntroContentProvider(dir, po, mcIntroPath).createIntroContent()

  private def logWarnings(po: String, warnings: List[ModuleCatalogWarning]): Unit =
    warnings.foreach { warning =>
      val module = warning.moduleId.fold("")(id => s" module=$id")
      logger.warn(s"module catalog $po warning ${warning.code}: ${warning.message}$module")
    }

  private def studyPlanSnippet(
      currentPO: String,
      modules: Vector[(ModuleProtocol, LocalDate)],
      isPreview: Boolean,
      config: ModuleCatalogStudyPlanConfig,
      specializations: List[models.core.IDLabel],
      collectWarning: ModuleCatalogWarning => Unit
  ): LatexContentSnippet =
    StudyPlanSnippet(
      currentPO,
      modules.map(m => (m._1.id.get, m._1.metadata)),
      NonEmptyList.fromList(config.sections),
      config.semesterSelections,
      config.genericModuleOccurrences,
      specializations,
      collectWarning,
      isPreview,
      messagesApi
    )
}
